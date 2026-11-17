package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.enums.TipoMovimentacao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimentacaoRequest(
        @NotNull(message = "produtoId é obrigatório")
        Long produtoId,

        @NotNull(message = "tipo é obrigatório (ENTRADA ou SAIDA)")
        TipoMovimentacao tipo,

        @NotNull(message = "quantidade é obrigatória")
        @Min(value = 1, message = "quantidade deve ser no mínimo 1")
        Integer quantidade,

        @Size(max = 160, message = "motivo deve ter no máximo 160 caracteres")
        String motivo
) {

    /** Saída manual (perda, quebra, ajuste) exige justificativa para a auditoria. */
    @AssertTrue(message = "motivo é obrigatório para SAIDA")
    public boolean isMotivoConsistente() {
        return tipo != TipoMovimentacao.SAIDA || (motivo != null && !motivo.isBlank());
    }
}