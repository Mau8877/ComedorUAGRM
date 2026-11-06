package com.comedoruagrm.backend.features.notificaciones.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.notificaciones.model.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Optional<Notificacion> findByCodigo(UUID codigo);

    long countByUsuarioId(Long usuarioId);
}
