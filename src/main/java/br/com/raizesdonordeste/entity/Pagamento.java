package br.com.raizesdonordeste.entity;

import br.com.raizesdonordeste.enums.FormaPagamento;
import br.com.raizesdonordeste.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Registro de uma tentativa de pagamento. Nunca guarda dados de cartão. */
@Entity
@Table(name = "pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FormaPagamento forma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusPagamento status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    /** Identificador da transação no gateway externo. */
    @Column(length = 80)
    private String referenciaExterna;

    @Column(length = 200)
    private String mensagem;

    /** JSON bruto devolvido pelo gateway. */
    @Column(length = 2000)
    private String payloadResposta;

    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        criadoEm = LocalDateTime.now();
    }
}