package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.Pagamento;
import br.com.raizesdonordeste.enums.FormaPagamento;
import br.com.raizesdonordeste.enums.StatusPagamento;
import br.com.raizesdonordeste.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponse(
        Long id,
        Long pedidoId,
        FormaPagamento forma,
        StatusPagamento status,
        String mensagem,
        BigDecimal valor,
        String referenciaExterna,
        String payload,
        StatusPedido statusPedido,
        LocalDateTime criadoEm
) {
    public static PagamentoResponse de(Pagamento p) {
        return new PagamentoResponse(p.getId(), p.getPedido().getId(), p.getForma(), p.getStatus(),
                p.getMensagem(), p.getValor(), p.getReferenciaExterna(), p.getPayloadResposta(),
                p.getPedido().getStatus(), p.getCriadoEm());
    }
}