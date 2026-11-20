package br.com.raizesdonordeste.service;

import br.com.raizesdonordeste.dto.PagamentoRequest;
import br.com.raizesdonordeste.dto.PagamentoResponse;
import br.com.raizesdonordeste.entity.Pagamento;
import br.com.raizesdonordeste.entity.Pedido;
import br.com.raizesdonordeste.enums.SimulacaoGateway;
import br.com.raizesdonordeste.enums.StatusPagamento;
import br.com.raizesdonordeste.enums.StatusPedido;
import br.com.raizesdonordeste.exception.NegocioException;
import br.com.raizesdonordeste.gateway.GatewayIndisponivelException;
import br.com.raizesdonordeste.gateway.PagamentoGateway;
import br.com.raizesdonordeste.gateway.RespostaGateway;
import br.com.raizesdonordeste.gateway.SolicitacaoPagamento;
import br.com.raizesdonordeste.repository.PagamentoRepository;
import br.com.raizesdonordeste.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoService pedidoService;
    private final PagamentoGateway gateway;
    private final FidelidadeService fidelidadeService;

    @Transactional
    public PagamentoResponse pagar(Long pedidoId, PagamentoRequest request, UsuarioAutenticado usuario) {
        // Bloqueia o pedido: pagamentos simultâneos do mesmo pedido ficam em fila
        Pedido pedido = pedidoService.buscarParaAtualizar(pedidoId, usuario);

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new NegocioException("PEDIDO_NAO_ACEITA_PAGAMENTO",
                    "O pedido está " + pedido.getStatus() + " e não aceita pagamento");
        }

        SimulacaoGateway simulacao = (request != null && request.simulacao() != null)
                ? request.simulacao()
                : SimulacaoGateway.APROVAR;

        Pagamento pagamento = Pagamento.builder()
                .pedido(pedido)
                .forma(pedido.getFormaPagamento())
                .valor(pedido.getTotal())
                .build();

        try {
            RespostaGateway resposta = gateway.cobrar(new SolicitacaoPagamento(
                    pedido.getId(), pedido.getTotal(), pedido.getFormaPagamento(), simulacao));

            pagamento.setStatus(resposta.status());
            pagamento.setReferenciaExterna(resposta.referenciaExterna());
            pagamento.setMensagem(resposta.mensagem());
            pagamento.setPayloadResposta(resposta.payload());

            if (resposta.status() == StatusPagamento.APROVADO) {
                pedido.avancarPara(StatusPedido.PAGO);
                fidelidadeService.acumularPontos(pedido);
            }
        } catch (GatewayIndisponivelException e) {
            // Tolerância a falhas: registra a tentativa e mantém o pedido pendente para nova tentativa
            log.warn("Gateway indisponível para o pedido {}: {}", pedidoId, e.getMessage());
            pagamento.setStatus(StatusPagamento.ERRO);
            pagamento.setMensagem("Gateway de pagamento indisponível. Tente novamente");
        }

        return PagamentoResponse.de(pagamentoRepository.save(pagamento));
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listar(Long pedidoId, UsuarioAutenticado usuario) {
        Pedido pedido = pedidoService.buscarEntidade(pedidoId, usuario);
        return pagamentoRepository.findByPedidoIdOrderByCriadoEmDescIdDesc(pedido.getId())
                .stream().map(PagamentoResponse::de).toList();
    }

    /** Chamado no cancelamento de um pedido já pago. */
    @Transactional
    public void estornarAprovados(Long pedidoId) {
        List<Pagamento> aprovados = pagamentoRepository.findByPedidoIdAndStatus(pedidoId, StatusPagamento.APROVADO);
        aprovados.forEach(p -> p.setStatus(StatusPagamento.ESTORNADO));
        pagamentoRepository.saveAll(aprovados);
    }
}