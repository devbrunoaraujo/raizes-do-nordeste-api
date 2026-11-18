package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.Pedido;
import br.com.raizesdonordeste.enums.CanalPedido;
import br.com.raizesdonordeste.enums.FormaPagamento;
import br.com.raizesdonordeste.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        CanalPedido canalPedido,
        StatusPedido status,
        Long unidadeId,
        Long clienteId,
        FormaPagamento formaPagamento,
        BigDecimal subtotal,
        BigDecimal desconto,
        BigDecimal total,
        List<ItemPedidoResponse> itens,
        LocalDateTime criadoEm
) {
    public static PedidoResponse de(Pedido p) {
        return new PedidoResponse(
                p.getId(),
                p.getCanalPedido(),
                p.getStatus(),
                p.getUnidade().getId(),
                p.getCliente() != null ? p.getCliente().getId() : null,
                p.getFormaPagamento(),
                p.getSubtotal(),
                p.getDesconto(),
                p.getTotal(),
                p.getItens().stream().map(ItemPedidoResponse::de).toList(),
                p.getCriadoEm());
    }
}