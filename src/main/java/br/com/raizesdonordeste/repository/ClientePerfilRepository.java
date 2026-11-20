package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.entity.ClientePerfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientePerfilRepository extends JpaRepository<ClientePerfil, Long> {

    Optional<ClientePerfil> findByUsuarioId(Long usuarioId);
}