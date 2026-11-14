package com.comedoruagrm.backend.features.notificaciones.repository;

import java.util.List;
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

    /** Un id interno de postulacion y su codigo publico (consulta propia: este modulo no lee `postulaciones`). */
    interface CodigoDePostulacion {

        Long getId();

        UUID getCodigo();
    }

    /** El codigo publico de la postulacion de un aviso, o vacio si ya no existe. */
    @Query(value = "select p.codigo_postulacion from postulacion p where p.id = :id and p.eliminado_en is null",
            nativeQuery = true)
    Optional<UUID> codigoDePostulacion(@Param("id") Long id);

    /** Los codigos publicos de varias postulaciones en una sola consulta (la bandeja no hace N+1). */
    @Query(value = "select p.id as id, p.codigo_postulacion as codigo from postulacion p "
            + "where p.id in (:ids) and p.eliminado_en is null", nativeQuery = true)
    List<CodigoDePostulacion> codigosDePostulaciones(@Param("ids") java.util.Collection<Long> ids);

    /** Marca como leidas todas las del usuario (no solo las cargadas en pantalla); devuelve cuantas cambio. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notificacion n set n.leida = true where n.usuario.id = :usuarioId and n.leida = false")
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId);

    /** Marca varios avisos como enviados con una sola sentencia (despacho por lotes, CU22). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notificacion n set n.enviadaEn = :cuando where n.id in :ids")
    int marcarEnviadas(@Param("ids") java.util.Collection<Long> ids, @Param("cuando") java.time.Instant cuando);

    /** Borrado fisico de todas las del usuario (la bandeja no tiene papelera). Devuelve cuantas borro. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notificacion n where n.usuario.id = :usuarioId")
    int eliminarTodas(@Param("usuarioId") Long usuarioId);
}
