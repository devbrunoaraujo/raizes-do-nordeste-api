package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.entity.Pedido;
import br.com.raizesdonordeste.enums.CanalPedido;
import br.com.raizesdonordeste.enums.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByIdempotencyKey(String idempotencyKey);

    /** Filtros opcionais: parâmetro nulo significa "sem filtro". */
    @Query("select p from Pedido p where (:canal is null or p.canalPedido = :canal) "
            + "and (:status is null or p.status = :status) "
            + "and (:unidadeId is null or p.unidade.id = :unidadeId) "
            + "and (:clienteId is null or p.cliente.id = :clienteId)")
    Page<Pedido> filtrar(@Param("canal") CanalPedido canal,
                         @Param("status") StatusPedido status,
                         @Param("unidadeId") Long unidadeId,
                         @Param("clienteId") Long clienteId,
                         Pageable pageable);
}