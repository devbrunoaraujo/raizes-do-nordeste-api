package br.com.raizesdonordeste.enums;

public enum StatusPedido {
    AGUARDANDO_PAGAMENTO,
    PAGO,
    EM_PREPARO,
    PRONTO,
    ENTREGUE,
    CANCELADO;

    /** Máquina de estados: o cancelamento só é possível até EM_PREPARO. */
    public boolean podeIrPara(StatusPedido destino) {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> destino == PAGO || destino == CANCELADO;
            case PAGO -> destino == EM_PREPARO || destino == CANCELADO;
            case EM_PREPARO -> destino == PRONTO || destino == CANCELADO;
            case PRONTO -> destino == ENTREGUE;
            case ENTREGUE, CANCELADO -> false;
        };
    }
}