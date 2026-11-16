package br.com.raizesdonordeste.security;

import br.com.raizesdonordeste.enums.Role;
import br.com.raizesdonordeste.exception.AcessoNegadoException;

/** Quem está fazendo a requisição. Disponível nos controllers via @AuthenticationPrincipal. */
public record UsuarioAutenticado(Long id, String email, Role role, Long unidadeId) {

    /** ADMIN atua em qualquer unidade; os demais, apenas na sua. */
    public void exigirAcessoAUnidade(Long unidadeAlvo) {
        boolean permitido = role == Role.ADMIN
                || (unidadeId != null && unidadeId.equals(unidadeAlvo));
        if (!permitido) {
            throw new AcessoNegadoException("Você só pode gerenciar a sua própria unidade");
        }
    }
}