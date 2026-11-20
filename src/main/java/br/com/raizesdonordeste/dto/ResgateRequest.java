package br.com.raizesdonordeste.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResgateRequest(
        @NotNull(message = "pedidoId é obrigatório")
        Long pedidoId,

        @NotNull(message = "pontos é obrigatório")
        @Min(value = 1, message = "pontos deve ser no mínimo 1")
        Integer pontos
) {
}