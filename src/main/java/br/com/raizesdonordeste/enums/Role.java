package br.com.raizesdonordeste.enums;

public enum Role {
    CLIENTE,
    ATENDENTE,
    COZINHA,
    GERENTE,
    ADMIN;

    /** Funcionários de loja precisam estar vinculados a uma unidade. */
    public boolean exigeUnidade() {
        return this == GERENTE || this == ATENDENTE || this == COZINHA;
    }
}