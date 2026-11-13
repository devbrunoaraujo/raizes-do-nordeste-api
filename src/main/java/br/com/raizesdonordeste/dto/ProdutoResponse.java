package br.com.raizesdonordeste.dto;

import br.com.raizesdonordeste.entity.Produto;

import java.math.BigDecimal;

public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        String categoria,
        BigDecimal preco,
        boolean ativo
) {
    public static ProdutoResponse de(Produto p) {
        return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(),
                p.getCategoria(), p.getPreco(), p.isAtivo());
    }
}