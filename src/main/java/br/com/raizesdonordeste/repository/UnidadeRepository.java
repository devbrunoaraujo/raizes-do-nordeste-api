package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    List<Unidade> findByAtivaTrue();
}