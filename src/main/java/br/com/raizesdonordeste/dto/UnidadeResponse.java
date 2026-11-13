package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.Unidade;

public record UnidadeResponse(
        Long id,
        String nome,
        String cidade,
        String uf,
        boolean ativa
) {
    public static UnidadeResponse de(Unidade u) {
        return new UnidadeResponse(u.getId(), u.getNome(), u.getCidade(), u.getUf(), u.isAtiva());
    }
}