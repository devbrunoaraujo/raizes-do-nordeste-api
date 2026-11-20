package br.com.raizesdonordeste.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record ClienteRequest(
        @NotNull(message = "dataNascimento é obrigatória")
        @Past(message = "dataNascimento deve ser uma data passada")
        LocalDate dataNascimento
) {
}