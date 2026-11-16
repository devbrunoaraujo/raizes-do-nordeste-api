package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.Estoque;

public record EstoqueResponse(
        Long produtoId,
        String produto,
        int quantidade
) {
    public static EstoqueResponse de(Estoque e) {
        return new EstoqueResponse(e.getProduto().getId(), e.getProduto().getNome(), e.getQuantidade());
    }
}