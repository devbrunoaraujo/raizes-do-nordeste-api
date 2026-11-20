package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.FidelidadeMovimento;
import br.com.raizesdonordeste.enums.TipoFidelidade;

import java.time.LocalDateTime;

public record MovimentoFidelidadeResponse(
        Long id,
        TipoFidelidade tipo,
        int pontos,
        Long pedidoId,
        String descricao,
        LocalDateTime criadoEm
) {
    public static MovimentoFidelidadeResponse de(FidelidadeMovimento m) {
        return new MovimentoFidelidadeResponse(m.getId(), m.getTipo(), m.getPontos(),
                m.getPedidoId(), m.getDescricao(), m.getCriadoEm());
    }
}