package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.enums.CanalPedido;
import br.com.raizesdonordeste.enums.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PedidoRequest(
        @NotNull(message = "canalPedido é obrigatório (APP, TOTEM, BALCAO, PICKUP ou WEB)")
        CanalPedido canalPedido,

        @NotNull(message = "unidadeId é obrigatório")
        Long unidadeId,

        Long clienteId,

        @NotNull(message = "formaPagamento é obrigatória")
        FormaPagamento formaPagamento,

        @NotEmpty(message = "o pedido precisa ter ao menos um item")
        @Size(max = 50, message = "o pedido pode ter no máximo 50 itens")
        @Valid
        List<ItemPedidoRequest> itens
) {
}