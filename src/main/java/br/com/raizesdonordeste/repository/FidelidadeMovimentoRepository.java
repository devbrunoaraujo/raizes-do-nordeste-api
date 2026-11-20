package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.entity.FidelidadeMovimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FidelidadeMovimentoRepository extends JpaRepository<FidelidadeMovimento, Long> {

    Page<FidelidadeMovimento> findByClienteIdOrderByCriadoEmDescIdDesc(Long clienteId, Pageable pageable);

    @Query("select coalesce(sum(m.pontos), 0) from FidelidadeMovimento m where m.cliente.id = :clienteId")
    Long saldo(@Param("clienteId") Long clienteId);
}