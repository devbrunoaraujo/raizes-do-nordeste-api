package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.enums.SimulacaoGateway;

/** O corpo é opcional. "simulacao" existe só por causa do mock; sem ele, o pagamento é aprovado. */
public record PagamentoRequest(SimulacaoGateway simulacao) {
}