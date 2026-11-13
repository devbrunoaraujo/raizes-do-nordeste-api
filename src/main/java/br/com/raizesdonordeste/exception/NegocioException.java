package br.com.raizesdonordeste.exception;

import lombok.Getter;

@Getter
public class NegocioException extends RuntimeException {

    private final String codigo;

    public NegocioException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }
}