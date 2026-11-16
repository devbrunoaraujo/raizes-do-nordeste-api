package br.com.raizesdonordeste.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CardapioItemAtualizacaoRequest(
        @DecimalMin(value = "0.00", message = "preço local não pode ser negativo")
        BigDecimal precoLocal,

        @NotNull(message = "disponivel é obrigatório")
        Boolean disponivel
) {
}