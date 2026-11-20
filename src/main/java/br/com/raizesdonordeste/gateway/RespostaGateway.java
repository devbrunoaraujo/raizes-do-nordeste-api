package br.com.raizesdonordeste.gateway;

import br.com.raizesdonordeste.enums.StatusPagamento;

/** O que o gateway devolve. */
public record RespostaGateway(
        StatusPagamento status,
        String referenciaExterna,
        String mensagem,
        String payload
) {
}