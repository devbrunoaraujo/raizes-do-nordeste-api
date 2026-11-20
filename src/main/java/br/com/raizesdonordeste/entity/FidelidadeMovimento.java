package br.com.raizesdonordeste.entity;

import br.com.raizesdonordeste.enums.TipoFidelidade;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Extrato de pontos. "pontos" tem sinal: positivo ganha, negativo gasta. O saldo é a soma. */
@Entity
@Table(name = "fidelidade_movimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FidelidadeMovimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClientePerfil cliente;

    /** Pedido de origem. Referência lógica, sem FK. */
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoFidelidade tipo;

    @Column(nullable = false)
    private int pontos;

    @Column(length = 120)
    private String descricao;

    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        criadoEm = LocalDateTime.now();
    }
}