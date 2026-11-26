package br.com.raizesdonordeste.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Trilha de auditoria. usuarioId é referência lógica (sem FK): o registro sobrevive
 * à anonimização do usuário. Nunca guardar nome, e-mail ou senha em "detalhes".
 */
@Entity
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_acao", columnList = "acao"),
        @Index(name = "idx_auditoria_entidade", columnList = "entidade,entidade_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;

    @Column(nullable = false, length = 60)
    private String acao;

    @Column(nullable = false, length = 60)
    private String entidade;

    private Long entidadeId;

    @Column(length = 500)
    private String detalhes;

    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() {
        criadoEm = LocalDateTime.now();
    }
}