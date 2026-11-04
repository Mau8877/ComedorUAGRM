package com.comedoruagrm.backend.features.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comedoruagrm.backend.features.auth.model.Sesion;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByTokenHash(String tokenHash);

    /**
     * Revoca las sesiones de un usuario que todavia no estan revocadas; devuelve cuantas cerro.
     * Hace flush antes (los cambios pendientes de la transaccion salen primero) pero NO limpia
     * el contexto de persistencia: quien llama sigue usando las entidades que ya cargo.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Sesion s set s.revocadaEn = :ahora where s.usuario.id = :usuarioId and s.revocadaEn is null")
    int revocarTodasDe(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);
}
