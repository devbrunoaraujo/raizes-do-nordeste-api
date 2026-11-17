package br.com.raizesdonordeste.exception;

/** Dado de entrada inválido que as anotações do DTO não cobrem (responde HTTP 422). */
public class ValidacaoException extends RuntimeException {

    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}