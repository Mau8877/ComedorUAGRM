package com.comedoruagrm.backend.features.postulaciones.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.postulaciones.model.Adjudicacion;

/**
 * Acceso a la tabla `adjudicacion` (CU05). Los conteos de cupos van por JDBC en
 * {@link AdjudicacionConsultaRepository}.
 */
public interface AdjudicacionRepository extends JpaRepository<Adjudicacion, Long> {

    Optional<Adjudicacion> findByPostulacionId(Long postulacionId);
}
