package br.com.raizesdonordeste.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cardapio_unidade",
        uniqueConstraints = @UniqueConstraint(columnNames = {"unidade_id", "produto_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardapioUnidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /** Preço específico desta unidade. Se for nulo, vale o preço do catálogo. */
    @Column(precision = 10, scale = 2)
    private BigDecimal precoLocal;

    @Column(nullable = false)
    @Builder.Default
    private boolean disponivel = true;

    /** Preço que o cliente realmente paga nesta unidade. */
    public BigDecimal precoEfetivo() {
        return precoLocal != null ? precoLocal : produto.getPreco();
    }
}