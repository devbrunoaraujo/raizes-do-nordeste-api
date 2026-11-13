package br.com.raizesdonordeste.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String codigo,
        String mensagem,
        String caminho,
        List<String> detalhes
) {
}