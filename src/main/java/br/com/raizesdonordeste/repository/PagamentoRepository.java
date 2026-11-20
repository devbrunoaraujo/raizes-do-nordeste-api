package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.entity.Pagamento;
import br.com.raizesdonordeste.enums.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByPedidoIdOrderByCriadoEmDescIdDesc(Long pedidoId);

    List<Pagamento> findByPedidoIdAndStatus(Long pedidoId, StatusPagamento status);
}