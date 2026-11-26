package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("select a from Auditoria a where (:acao is null or a.acao = :acao) "
            + "and (:entidade is null or a.entidade = :entidade) "
            + "order by a.criadoEm desc, a.id desc")
    Page<Auditoria> filtrar(@Param("acao") String acao,
                            @Param("entidade") String entidade,
                            Pageable pageable);
}