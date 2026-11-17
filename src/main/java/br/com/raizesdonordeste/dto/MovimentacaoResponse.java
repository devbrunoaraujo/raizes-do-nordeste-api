package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.MovimentacaoEstoque;
import br.com.raizesdonordeste.enums.TipoMovimentacao;

import java.time.LocalDateTime;

public record MovimentacaoResponse(
        Long id,
        Long produtoId,
        TipoMovimentacao tipo,
        int quantidade,
        int saldoApos,
        String motivo,
        Long pedidoId,
        LocalDateTime criadoEm
) {
    public static MovimentacaoResponse de(MovimentacaoEstoque m) {
        return new MovimentacaoResponse(m.getId(), m.getEstoque().getProduto().getId(), m.getTipo(),
                m.getQuantidade(), m.getSaldoApos(), m.getMotivo(), m.getPedidoId(), m.getCriadoEm());
    }
}