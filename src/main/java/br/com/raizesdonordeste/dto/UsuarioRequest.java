package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.enums.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        @Size(max = 160, message = "email deve ter no máximo 160 caracteres")
        String email,

        @NotBlank(message = "senha é obrigatória")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String senha,

        @NotNull(message = "role é obrigatório")
        Role role,

        Long unidadeId
) {

    /** Validação entre campos: GERENTE, ATENDENTE e COZINHA exigem unidadeId. */
    @AssertTrue(message = "unidadeId é obrigatório para GERENTE, ATENDENTE e COZINHA")
    public boolean isUnidadeConsistente() {
        return role == null || !role.exigeUnidade() || unidadeId != null;
    }
}