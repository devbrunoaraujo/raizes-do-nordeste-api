package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.ClientePerfil;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long usuarioId,
        String nome,
        String email,
        LocalDate dataNascimento,
        boolean consentimentoFidelidade,
        LocalDateTime consentimentoEm
) {
    public static ClienteResponse de(ClientePerfil p) {
        return new ClienteResponse(p.getUsuario().getId(), p.getUsuario().getNome(),
                p.getUsuario().getEmail(), p.getDataNascimento(),
                p.isConsentimentoFidelidade(), p.getConsentimentoEm());
    }
}