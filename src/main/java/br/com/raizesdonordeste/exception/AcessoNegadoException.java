package br.com.raizesdonordeste.exception;

/** Usuário autenticado tentando agir fora do seu escopo (responde HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}