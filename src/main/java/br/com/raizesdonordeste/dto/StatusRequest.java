package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.enums.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "status é obrigatório")
        StatusPedido status
) {
}