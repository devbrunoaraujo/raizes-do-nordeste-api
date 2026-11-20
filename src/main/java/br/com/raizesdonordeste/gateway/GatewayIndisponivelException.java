package br.com.raizesdonordeste.gateway;

/** O gateway não respondeu (timeout, queda, erro de rede). */
public class GatewayIndisponivelException extends RuntimeException {

    public GatewayIndisponivelException(String mensagem) {
        super(mensagem);
    }
}