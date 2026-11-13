package br.com.raizesdonordeste.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
        String nome,

        @Size(max = 300, message = "descrição deve ter no máximo 300 caracteres")
        String descricao,

        @NotBlank(message = "categoria é obrigatória")
        @Size(max = 60, message = "categoria deve ter no máximo 60 caracteres")
        String categoria,

        @NotNull(message = "preço é obrigatório")
        @DecimalMin(value = "0.00", message = "preço não pode ser negativo")
        BigDecimal preco
) {
}