package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.ItemPedidoRequest;
import br.com.raizesdonordeste.dto.PedidoRequest;
import br.com.raizesdonordeste.dto.PedidoResponse;
import br.com.raizesdonordeste.entity.*;
import br.com.raizesdonordeste.enums.Role;
import br.com.raizesdonordeste.exception.NegocioException;
import br.com.raizesdonordeste.exception.ValidacaoException;
import br.com.raizesdonordeste.repository.PedidoRepository;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.enums.CanalPedido;
import br.com.raizesdonordeste.enums.StatusPedido;
import br.com.raizesdonordeste.exception.AcessoNegadoException;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import br.com.raizesdonordeste.exception.AcessoNegadoException;
import java.util.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final int TAMANHO_MAXIMO_CHAVE = 80;
    private static final int LIMITE_MAXIMO = 100;

    private final PedidoRepository pedidoRepository;
    private final UnidadeService unidadeService;
    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;
    private final CardapioService cardapioService;
    private final EstoqueService estoqueService;

    /** criado = false significa que a chave de idempotência já tinha gerado este pedido. */
    public record Resultado(PedidoResponse pedido, boolean criado) {
    }

    @Transactional
    public Resultado criar(PedidoRequest request, String chaveBruta, UsuarioAutenticado usuario) {
        String chave = normalizarChave(chaveBruta);

        // 1. Idempotência: a mesma chave devolve o pedido original, sem duplicar nada
        if (chave != null) {
            Optional<Pedido> existente = pedidoRepository.findByIdempotencyKey(chave);
            if (existente.isPresent()) {
                Pedido original = existente.get();
                if (!usuario.id().equals(original.getCriadoPorId())) {
                    throw new NegocioException("IDEMPOTENCY_KEY_EM_USO",
                            "Esta chave de idempotência já foi usada por outro usuário");
                }
                return new Resultado(PedidoResponse.de(original), false);
            }
        }

        // 2. Escopo: cliente escolhe qualquer loja; funcionário só atua na própria
        if (usuario.role() != Role.CLIENTE) {
            usuario.exigirAcessoAUnidade(request.unidadeId());
        }

        // 3. Unidade e cliente
        Unidade unidade = unidadeService.buscarEntidade(request.unidadeId());
        if (!unidade.isAtiva()) {
            throw new NegocioException("UNIDADE_INATIVA", "A unidade não está em operação");
        }

        Pedido pedido = new Pedido();
        pedido.setUnidade(unidade);
        pedido.setCliente(resolverCliente(request.clienteId(), usuario));
        pedido.setCanalPedido(request.canalPedido());
        pedido.setFormaPagamento(request.formaPagamento());
        pedido.setIdempotencyKey(chave);
        pedido.setCriadoPorId(usuario.id());

        // 4. Itens: valida produto, disponibilidade e congela o preço
        for (Map.Entry<Long, Integer> linha : consolidar(request.itens()).entrySet()) {
            Produto produto = produtoService.buscarEntidade(linha.getKey());
            CardapioUnidade itemCardapio = cardapioService.buscarItemVendavel(unidade.getId(), produto);
            pedido.adicionarItem(produto, linha.getValue(), itemCardapio.precoEfetivo());
        }

        // 5. Totais (promoções entram aqui no futuro, no lugar do ZERO)
        pedido.calcularTotais(BigDecimal.ZERO);
        pedido = pedidoRepository.save(pedido);

        // 6. Baixa de estoque: se faltar saldo, a transação inteira é desfeita
        for (ItemPedido item : pedido.getItens()) {
            estoqueService.baixarParaPedido(unidade.getId(), item.getProduto(),
                    item.getQuantidade(), pedido.getId(), usuario.id());
        }

        return new Resultado(PedidoResponse.de(pedido), true);
    }

    private Usuario resolverCliente(Long clienteIdInformado, UsuarioAutenticado usuario) {
        if (usuario.role() == Role.CLIENTE) {
            // Cliente só compra para si: o clienteId do corpo é ignorado
            return usuarioService.buscarEntidade(usuario.id());
        }
        if (clienteIdInformado == null) {
            return null; // venda anônima
        }
        Usuario cliente = usuarioService.buscarEntidade(clienteIdInformado);
        if (cliente.getRole() != Role.CLIENTE) {
            throw new NegocioException("CLIENTE_INVALIDO", "O usuário informado não é um cliente");
        }
        return cliente;
    }

    /** Soma as quantidades se o mesmo produto aparecer em mais de uma linha. */
    private Map<Long, Integer> consolidar(List<ItemPedidoRequest> itens) {
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (ItemPedidoRequest item : itens) {
            quantidades.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }
        return quantidades;
    }

    private String normalizarChave(String chave) {
        if (chave == null || chave.isBlank()) return null;
        String limpa = chave.trim();
        if (limpa.length() > TAMANHO_MAXIMO_CHAVE) {
            throw new ValidacaoException("Idempotency-Key deve ter no máximo " + TAMANHO_MAXIMO_CHAVE + " caracteres");
        }
        return limpa;
    }

    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> listar(CanalPedido canal, StatusPedido status, Long unidadeId,
                                               int page, int limit, UsuarioAutenticado usuario) {
        Long unidadeFiltro = unidadeId;
        Long clienteFiltro = null;

        switch (usuario.role()) {
            case ADMIN -> { /* enxerga a rede toda, filtros opcionais */ }
            case CLIENTE -> clienteFiltro = usuario.id(); // só os próprios pedidos
            default -> {
                // ATENDENTE, COZINHA e GERENTE: sempre restritos à própria unidade
                if (usuario.unidadeId() == null) {
                    throw new AcessoNegadoException("Usuário sem unidade vinculada");
                }
                if (unidadeId != null) {
                    usuario.exigirAcessoAUnidade(unidadeId);
                }
                unidadeFiltro = usuario.unidadeId();
            }
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(limit, 1), LIMITE_MAXIMO),
                Sort.by(Sort.Direction.DESC, "criadoEm"));

        return PageResponse.de(pedidoRepository
                .filtrar(canal, status, unidadeFiltro, clienteFiltro, pageable)
                .map(PedidoResponse::de));
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id, UsuarioAutenticado usuario) {
        return PedidoResponse.de(buscarEntidade(id, usuario));
    }

    /** Pedido que o usuário tem permissão de enxergar. */
    @Transactional(readOnly = true)
    public Pedido buscarEntidade(Long id, UsuarioAutenticado usuario) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
        verificarAcesso(pedido, usuario);
        return pedido;
    }

    /** Igual ao anterior, mas com bloqueio de escrita: use nas operações que alteram o pedido. */
    @Transactional
    public Pedido buscarParaAtualizar(Long id, UsuarioAutenticado usuario) {
        Pedido pedido = pedidoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
        verificarAcesso(pedido, usuario);
        return pedido;
    }

    /** Quem pode levar o pedido para cada status manual. PAGO e CANCELADO têm fluxo próprio. */
    private static final Map<StatusPedido, Set<Role>> QUEM_PODE_ALTERAR = Map.of(
            StatusPedido.EM_PREPARO, Set.of(Role.COZINHA, Role.GERENTE, Role.ADMIN),
            StatusPedido.PRONTO, Set.of(Role.COZINHA, Role.GERENTE, Role.ADMIN),
            StatusPedido.ENTREGUE, Set.of(Role.ATENDENTE, Role.GERENTE, Role.ADMIN));

    @Transactional
    public PedidoResponse atualizarStatus(Long id, StatusPedido novoStatus, UsuarioAutenticado usuario) {
        Set<Role> permitidos = QUEM_PODE_ALTERAR.get(novoStatus);
        if (permitidos == null) {
            throw new ValidacaoException("O status " + novoStatus + " não pode ser definido manualmente. "
                    + "Use /pagamentos para PAGO e /cancelamento para CANCELADO");
        }
        if (!permitidos.contains(usuario.role())) {
            throw new AcessoNegadoException("Seu perfil não pode mover o pedido para " + novoStatus);
        }

        Pedido pedido = buscarParaAtualizar(id, usuario);
        pedido.avancarPara(novoStatus); // 409 se a transição for inválida
        return PedidoResponse.de(pedido);
    }

    private void verificarAcesso(Pedido pedido, UsuarioAutenticado usuario) {
        if (usuario.role() == Role.CLIENTE) {
            boolean dono = pedido.getCliente() != null && pedido.getCliente().getId().equals(usuario.id());
            if (!dono) {
                // 404 (e não 403) para não revelar que o pedido de outra pessoa existe
                throw new RecursoNaoEncontradoException("Pedido", pedido.getId());
            }
        } else {
            usuario.exigirAcessoAUnidade(pedido.getUnidade().getId());
        }
    }
}