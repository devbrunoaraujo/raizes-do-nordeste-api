package br.com.raizesdonordeste.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 80)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativa = true;
}