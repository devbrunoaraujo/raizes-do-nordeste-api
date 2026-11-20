package br.com.raizesdonordeste.gateway;

public interface PagamentoGateway {

    /**
     * Solicita a cobrança ao serviço externo.
     *
     * @throws GatewayIndisponivelException se o serviço não responder
     */
    RespostaGateway cobrar(SolicitacaoPagamento solicitacao);
}