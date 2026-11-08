package com.comedoruagrm.backend.features.postulaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;

/** Acceso a la tabla `postulacion_historial`. Las mas recientes de una postulacion van primero. */
public interface PostulacionHistorialRepository extends JpaRepository<PostulacionHistorial, Long> {

    List<PostulacionHistorial> findByPostulacionIdOrderByCreadoEnDescIdDesc(Long postulacionId);
}
