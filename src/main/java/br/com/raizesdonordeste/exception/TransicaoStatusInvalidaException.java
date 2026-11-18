package br.com.raizesdonordeste.exception;

import br.com.raizesdonordeste.enums.StatusPedido;

public class TransicaoStatusInvalidaException extends NegocioException {

    public TransicaoStatusInvalidaException(StatusPedido de, StatusPedido para) {
        super("TRANSICAO_STATUS_INVALIDA", "Não é possível mudar o pedido de " + de + " para " + para);
    }
}