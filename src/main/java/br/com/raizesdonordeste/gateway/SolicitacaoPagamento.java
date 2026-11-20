package br.com.raizesdonordeste.gateway;

import br.com.raizesdonordeste.enums.FormaPagamento;
import br.com.raizesdonordeste.enums.SimulacaoGateway;

import java.math.BigDecimal;

/** O que o sistema envia ao gateway. */
public record SolicitacaoPagamento(
        Long pedidoId,
        BigDecimal valor,
        FormaPagamento forma,
        SimulacaoGateway simulacao
) {
}