package br.com.raizesdonordeste.gateway;

import br.com.raizesdonordeste.enums.StatusPagamento;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Gateway simulado: não conversa com nenhum provedor real. */
@Component
public class PagamentoGatewayMock implements PagamentoGateway {

    @Override
    public RespostaGateway cobrar(SolicitacaoPagamento solicitacao) {
        String transacao = "MOCK-" + UUID.randomUUID();

        return switch (solicitacao.simulacao()) {
            case APROVAR -> new RespostaGateway(
                    StatusPagamento.APROVADO,
                    transacao,
                    "Pagamento aprovado",
                    "{\"transactionId\":\"" + transacao + "\",\"status\":\"APPROVED\",\"amount\":"
                            + solicitacao.valor().toPlainString() + "}");

            case RECUSAR -> new RespostaGateway(
                    StatusPagamento.RECUSADO,
                    transacao,
                    "Pagamento recusado pelo emissor (simulação)",
                    "{\"transactionId\":\"" + transacao + "\",\"status\":\"DECLINED\","
                            + "\"reason\":\"insufficient_funds\"}");

            case TIMEOUT -> throw new GatewayIndisponivelException(
                    "O gateway de pagamento não respondeu a tempo");
        };
    }
}