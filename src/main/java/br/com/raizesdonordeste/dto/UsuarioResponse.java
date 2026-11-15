package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.Usuario;
import br.com.raizesdonordeste.enums.Role;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Role role,
        Long unidadeId,
        boolean ativo
) {
    public static UsuarioResponse de(Usuario u) {
        Long unidadeId = u.getUnidade() != null ? u.getUnidade().getId() : null;
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getRole(), unidadeId, u.isAtivo());
    }
}