package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.CardapioUnidade;

import java.math.BigDecimal;

public record CardapioItemResponse(
        Long produtoId,
        String nome,
        String descricao,
        String categoria,
        BigDecimal preco,
        boolean disponivel
) {
    public static CardapioItemResponse de(CardapioUnidade c) {
        return new CardapioItemResponse(
                c.getProduto().getId(),
                c.getProduto().getNome(),
                c.getProduto().getDescricao(),
                c.getProduto().getCategoria(),
                c.precoEfetivo(),
                c.isDisponivel());
    }
}