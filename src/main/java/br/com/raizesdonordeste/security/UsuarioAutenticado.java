package br.com.raizesdonordeste.security;

import br.com.raizesdonordeste.enums.Role;

/** Quem está fazendo a requisição. Disponível para os controllers e services (auditoria, pedidos). */
public record UsuarioAutenticado(Long id, String email, Role role) {
}