package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.ItemPedido;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long produtoId,
        String produto,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
    public static ItemPedidoResponse de(ItemPedido i) {
        return new ItemPedidoResponse(i.getProduto().getId(), i.getProduto().getNome(),
                i.getQuantidade(), i.getPrecoUnitario(), i.subtotal());
    }
}