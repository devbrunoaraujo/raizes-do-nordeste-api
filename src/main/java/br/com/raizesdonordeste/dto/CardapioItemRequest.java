package br.com.raizesdonordeste.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CardapioItemRequest(
        @NotNull(message = "produtoId é obrigatório")
        Long produtoId,

        @DecimalMin(value = "0.00", message = "preço local não pode ser negativo")
        BigDecimal precoLocal
) {
}