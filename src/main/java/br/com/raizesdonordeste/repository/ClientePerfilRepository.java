package br.com.raizesdonordeste.repository;

import br.com.raizesdonordeste.entity.ClientePerfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface ClientePerfilRepository extends JpaRepository<ClientePerfil, Long> {

    Optional<ClientePerfil> findByUsuarioId(Long usuarioId);

    /** Bloqueia o perfil: dois resgates simultâneos do mesmo cliente não gastam o mesmo saldo duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClientePerfil c where c.usuario.id = :usuarioId")
    Optional<ClientePerfil> buscarParaAtualizar(@Param("usuarioId") Long usuarioId);
}