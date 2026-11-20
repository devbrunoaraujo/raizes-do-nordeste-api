package br.com.raizesdonordeste.dto;

import java.math.BigDecimal;

public record SaldoResponse(
        boolean consentimentoFidelidade,
        long saldo,
        BigDecimal valorPorPonto,
        int resgateMinimo
) {
}