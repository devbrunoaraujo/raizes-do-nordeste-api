package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.enums.Role;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Role role
) {
}