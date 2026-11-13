package br.com.raizesdonordeste.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnidadeRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "cidade é obrigatória")
        @Size(max = 80, message = "cidade deve ter no máximo 80 caracteres")
        String cidade,

        @NotBlank(message = "uf é obrigatória")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "uf deve ter exatamente 2 letras")
        String uf
) {
}