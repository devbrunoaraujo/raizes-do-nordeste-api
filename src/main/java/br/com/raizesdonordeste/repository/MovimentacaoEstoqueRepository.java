package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.entity.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    Page<MovimentacaoEstoque> findByEstoqueIdOrderByCriadoEmDescIdDesc(Long estoqueId, Pageable pageable);
}