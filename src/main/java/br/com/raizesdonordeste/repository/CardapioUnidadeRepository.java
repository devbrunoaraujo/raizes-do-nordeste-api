package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.entity.CardapioUnidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardapioUnidadeRepository extends JpaRepository<CardapioUnidade, Long> {

    Page<CardapioUnidade> findByUnidadeIdAndDisponivelTrueAndProdutoAtivoTrue(Long unidadeId, Pageable pageable);

    Optional<CardapioUnidade> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

    boolean existsByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);
}