package br.com.raizesdonordeste.exception;

public class EstoqueInsuficienteException extends NegocioException {

    public EstoqueInsuficienteException(Long produtoId, int disponivel, int solicitado) {
        super("ESTOQUE_INSUFICIENTE",
                "Estoque insuficiente para o produto " + produtoId
                        + " (disponível: " + disponivel + ", solicitado: " + solicitado + ")");
    }
}