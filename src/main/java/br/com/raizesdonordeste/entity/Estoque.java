package br.com.raizesdonordeste.entity;

import br.com.raizesdonordeste.exception.EstoqueInsuficienteException;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estoque",
        uniqueConstraints = @UniqueConstraint(columnNames = {"unidade_id", "produto_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Version
    private Long versao;

    public void baixar(int qtd) {
        if (qtd <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        if (quantidade < qtd) {
            throw new EstoqueInsuficienteException(produto.getId(), quantidade, qtd);
        }
        quantidade -= qtd;
    }

    public void repor(int qtd) {
        if (qtd <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        quantidade += qtd;
    }
}