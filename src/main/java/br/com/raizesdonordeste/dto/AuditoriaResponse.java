package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.Auditoria;

import java.time.LocalDateTime;

public record AuditoriaResponse(
        Long id,
        Long usuarioId,
        String acao,
        String entidade,
        Long entidadeId,
        String detalhes,
        LocalDateTime criadoEm
) {
    public static AuditoriaResponse de(Auditoria a) {
        return new AuditoriaResponse(a.getId(), a.getUsuarioId(), a.getAcao(), a.getEntidade(),
                a.getEntidadeId(), a.getDetalhes(), a.getCriadoEm());
    }
}