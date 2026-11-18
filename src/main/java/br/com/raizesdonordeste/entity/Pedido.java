package br.com.raizesdonordeste.entity;

import br.com.raizesdonordeste.enums.CanalPedido;
import br.com.raizesdonordeste.enums.FormaPagamento;
import br.com.raizesdonordeste.enums.StatusPedido;
import br.com.raizesdonordeste.exception.TransicaoStatusInvalidaException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido", indexes = {
        @Index(name = "idx_pedido_canal_status", columnList = "canal_pedido,status"),
        @Index(name = "idx_pedido_unidade_data", columnList = "unidade_id,criado_em")})
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nulo permite venda anônima (balcão ou totem sem identificação). */
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CanalPedido canalPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(unique = true, length = 80)
    private String idempotencyKey;

    /** Quem registrou o pedido (o próprio cliente ou o atendente). Referência lógica, sem FK. */
    private Long criadoPorId;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public void adicionarItem(Produto produto, int quantidade, BigDecimal precoUnitario) {
        ItemPedido item = new ItemPedido();
        item.setPedido(this);
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(precoUnitario);
        itens.add(item);
    }

    /** Recalcula subtotal e total. O desconto nunca ultrapassa o subtotal. */
    public void calcularTotais(BigDecimal descontoAplicado) {
        subtotal = itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal d = descontoAplicado == null ? BigDecimal.ZERO : descontoAplicado;
        desconto = d.min(subtotal);
        total = subtotal.subtract(desconto);
    }

    public void avancarPara(StatusPedido novo) {
        if (!status.podeIrPara(novo)) {
            throw new TransicaoStatusInvalidaException(status, novo);
        }
        status = novo;
    }

    @PrePersist
    void aoCriar() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }
}