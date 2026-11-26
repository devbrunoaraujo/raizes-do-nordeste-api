package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.MovimentoFidelidadeResponse;
import br.com.raizesdonordeste.dto.PageResponse;
import br.com.raizesdonordeste.dto.PedidoResponse;
import br.com.raizesdonordeste.dto.SaldoResponse;
import br.com.raizesdonordeste.entity.ClientePerfil;
import br.com.raizesdonordeste.entity.FidelidadeMovimento;
import br.com.raizesdonordeste.entity.Pedido;
import br.com.raizesdonordeste.enums.StatusPedido;
import br.com.raizesdonordeste.enums.TipoFidelidade;
import br.com.raizesdonordeste.exception.NegocioException;
import br.com.raizesdonordeste.exception.ValidacaoException;
import br.com.raizesdonordeste.repository.ClientePerfilRepository;
import br.com.raizesdonordeste.repository.FidelidadeMovimentoRepository;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FidelidadeService {

    /** 1 ponto vale R$ 0,10 de desconto. */
    public static final BigDecimal VALOR_PONTO = new BigDecimal("0.10");
    public static final int RESGATE_MINIMO = 10;
    private static final int LIMITE_MAXIMO = 100;

    private final FidelidadeMovimentoRepository movimentoRepository;
    private final ClientePerfilRepository perfilRepository;
    private final ClienteService clienteService;
    private final PedidoService pedidoService;
    private final AuditoriaService auditoriaService; // AUDITORIA

    @Transactional
    public SaldoResponse consultarSaldo(UsuarioAutenticado usuario) {
        return montarSaldo(clienteService.obterOuCriarPerfil(usuario.id()));
    }

    @Transactional
    public PageResponse<MovimentoFidelidadeResponse> historico(UsuarioAutenticado usuario, int page, int limit) {
        ClientePerfil perfil = clienteService.obterOuCriarPerfil(usuario.id());
        Pageable pageable = PageRequest.of(
                Math.max(page, 1) - 1,
                Math.min(Math.max(limit, 1), LIMITE_MAXIMO));
        return PageResponse.de(movimentoRepository
                .findByClienteIdOrderByCriadoEmDescIdDesc(perfil.getId(), pageable)
                .map(MovimentoFidelidadeResponse::de));
    }

    @Transactional
    public SaldoResponse darConsentimento(UsuarioAutenticado usuario) {
        ClientePerfil perfil = clienteService.obterOuCriarPerfil(usuario.id());
        if (!perfil.isConsentimentoFidelidade()) {
            perfil.darConsentimento();
            perfilRepository.save(perfil);
            auditoriaService.registrar(usuario.id(), "CONSENTIMENTO_CONCEDIDO", "ClientePerfil",
                    perfil.getId(), null); // AUDITORIA: só quando há mudança real
        }
        return montarSaldo(perfil);
    }

    @Transactional
    public SaldoResponse revogarConsentimento(UsuarioAutenticado usuario) {
        ClientePerfil perfil = clienteService.obterOuCriarPerfil(usuario.id());
        if (perfil.isConsentimentoFidelidade()) {
            perfil.revogarConsentimento();
            perfilRepository.save(perfil);
            auditoriaService.registrar(usuario.id(), "CONSENTIMENTO_REVOGADO", "ClientePerfil",
                    perfil.getId(), null); // AUDITORIA: só quando há mudança real
        }
        return montarSaldo(perfil);
    }

    /** Chamado quando o pagamento é aprovado. Só acumula se o cliente consentiu. */
    @Transactional
    public void acumularPontos(Pedido pedido) {
        if (pedido.getCliente() == null) return; // venda anônima não pontua

        ClientePerfil perfil = perfilRepository.findByUsuarioId(pedido.getCliente().getId())
                .filter(ClientePerfil::isConsentimentoFidelidade)
                .orElse(null);
        if (perfil == null) return;

        if (movimentoRepository.existsByPedidoIdAndTipo(pedido.getId(), TipoFidelidade.ACUMULO)) return;

        int pontos = pedido.getTotal().setScale(0, RoundingMode.DOWN).intValue();
        if (pontos <= 0) return;

        movimentoRepository.save(FidelidadeMovimento.builder()
                .cliente(perfil)
                .pedidoId(pedido.getId())
                .tipo(TipoFidelidade.ACUMULO)
                .pontos(pontos)
                .descricao("Compra - pedido #" + pedido.getId())
                .build());
    }

    /** Chamado no cancelamento: desfaz o que o pedido ganhou e devolve o que o pedido gastou. */
    @Transactional
    public void estornarPedido(Pedido pedido) {
        if (pedido.getCliente() == null) return;

        long liquido = movimentoRepository.saldoDoPedido(pedido.getId());
        if (liquido == 0) return;

        perfilRepository.findByUsuarioId(pedido.getCliente().getId()).ifPresent(perfil ->
                movimentoRepository.save(FidelidadeMovimento.builder()
                        .cliente(perfil)
                        .pedidoId(pedido.getId())
                        .tipo(TipoFidelidade.ESTORNO)
                        .pontos((int) -liquido)
                        .descricao("Cancelamento - pedido #" + pedido.getId())
                        .build()));
    }

    @Transactional
    public PedidoResponse resgatar(Long pedidoId, int pontos, UsuarioAutenticado usuario) {
        // Ordem fixa de bloqueios (pedido, depois perfil): evita deadlock com pagamento e cancelamento
        Pedido pedido = pedidoService.buscarParaAtualizar(pedidoId, usuario);

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new NegocioException("PEDIDO_NAO_ACEITA_RESGATE",
                    "O resgate só pode ser aplicado a pedidos aguardando pagamento");
        }
        if (pedido.getPontosResgatados() > 0) {
            throw new NegocioException("RESGATE_JA_APLICADO", "Este pedido já recebeu um resgate de pontos");
        }
        if (pontos < RESGATE_MINIMO) {
            throw new ValidacaoException("O resgate mínimo é de " + RESGATE_MINIMO + " pontos");
        }

        ClientePerfil perfil = perfilRepository.buscarParaAtualizar(usuario.id())
                .filter(ClientePerfil::isConsentimentoFidelidade)
                .orElseThrow(() -> new NegocioException("CONSENTIMENTO_NECESSARIO",
                        "Aceite o programa de fidelidade para resgatar pontos"));

        long saldo = movimentoRepository.saldo(perfil.getId());
        if (saldo < pontos) {
            throw new NegocioException("SALDO_PONTOS_INSUFICIENTE",
                    "Saldo insuficiente (disponível: " + saldo + ", solicitado: " + pontos + ")");
        }

        BigDecimal desconto = VALOR_PONTO.multiply(BigDecimal.valueOf(pontos));
        if (desconto.compareTo(pedido.getSubtotal()) > 0) {
            throw new NegocioException("RESGATE_EXCEDE_VALOR_DO_PEDIDO",
                    "O desconto de R$ " + desconto + " é maior que o valor do pedido");
        }

        pedido.aplicarResgate(pontos, desconto);
        movimentoRepository.save(FidelidadeMovimento.builder()
                .cliente(perfil)
                .pedidoId(pedido.getId())
                .tipo(TipoFidelidade.RESGATE)
                .pontos(-pontos)
                .descricao("Resgate - pedido #" + pedido.getId())
                .build());

        auditoriaService.registrar(usuario.id(), "RESGATE_PONTOS", "Pedido", pedido.getId(),
                "pontos=" + pontos + ", desconto=" + desconto); // AUDITORIA

        return PedidoResponse.de(pedido);
    }

    private SaldoResponse montarSaldo(ClientePerfil perfil) {
        long saldo = movimentoRepository.saldo(perfil.getId());
        return new SaldoResponse(perfil.isConsentimentoFidelidade(), saldo, VALOR_PONTO, RESGATE_MINIMO);
    }
}