package br.com.raizesdonordeste.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(
        @NotNull(message = "produtoId é obrigatório")
        Long produtoId,

        @NotNull(message = "quantidade é obrigatória")
        @Min(value = 1, message = "quantidade deve ser no mínimo 1")
        @Max(value = 99, message = "quantidade deve ser no máximo 99")
        Integer quantidade
) {
}