package br.com.raizesdonordeste.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Dados do cliente além do login, incluindo o consentimento LGPD para a fidelidade. */
@Entity
@Table(name = "cliente_perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientePerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    private LocalDate dataNascimento;

    @Column(nullable = false)
    private boolean consentimentoFidelidade;

    private LocalDateTime consentimentoEm;

    private LocalDateTime consentimentoRevogadoEm;

    private LocalDateTime anonimizadoEm;

    public void darConsentimento() {
        consentimentoFidelidade = true;
        consentimentoEm = LocalDateTime.now();
        consentimentoRevogadoEm = null;
    }

    public void revogarConsentimento() {
        consentimentoFidelidade = false;
        consentimentoRevogadoEm = LocalDateTime.now();
    }

    /** Remove os dados pessoais deste perfil. */
    public void anonimizar() {
        dataNascimento = null;
        consentimentoFidelidade = false;
        anonimizadoEm = LocalDateTime.now();
    }
}