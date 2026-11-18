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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final int TAMANHO_MAXIMO_CHAVE = 80;

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
}