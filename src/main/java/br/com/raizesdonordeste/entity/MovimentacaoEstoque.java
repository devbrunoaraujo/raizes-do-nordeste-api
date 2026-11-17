package br.com.raizesdonordeste.entity;

import br.com.raizesdonordeste.enums.TipoMovimentacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacao_estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estoque_id", nullable = false)
    private Estoque estoque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentacao tipo;

    @Column(nullable = false)
    private int quantidade;

    /** Saldo do estoque logo depois desta movimentação (facilita o extrato e a auditoria). */
    @Column(nullable = false)
    private int saldoApos;

    @Column(length = 160)
    private String motivo;

    /** Preenchido quando a saída decorre de um pedido. Referência lógica, sem FK. */
    private Long pedidoId;

    /** Quem realizou a movimentação. Referência lógica, sem FK. */
    private Long usuarioId;

    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        criadoEm = LocalDateTime.now();
    }
}