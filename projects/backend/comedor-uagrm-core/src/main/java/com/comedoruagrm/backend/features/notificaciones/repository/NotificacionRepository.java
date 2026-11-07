package com.comedoruagrm.backend.features.notificaciones.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comedoruagrm.backend.features.notificaciones.model.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Optional<Notificacion> findByCodigo(UUID codigo);

    Optional<Notificacion> findByCodigoAndUsuarioId(UUID codigo, Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    Page<Notificacion> findByUsuarioId(Long usuarioId, Pageable pageable);

    /** Marca como leidas todas las del usuario (no solo las cargadas en pantalla); devuelve cuantas cambio. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notificacion n set n.leida = true where n.usuario.id = :usuarioId and n.leida = false")
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId);

    /** Borrado fisico de todas las del usuario (la bandeja no tiene papelera). Devuelve cuantas borro. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notificacion n where n.usuario.id = :usuarioId")
    int eliminarTodas(@Param("usuarioId") Long usuarioId);
}
