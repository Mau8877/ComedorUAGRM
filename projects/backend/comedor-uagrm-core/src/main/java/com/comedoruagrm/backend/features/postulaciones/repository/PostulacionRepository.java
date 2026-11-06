package com.comedoruagrm.backend.features.postulaciones.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;

/**
 * Acceso a la tabla `postulacion`. La lista priorizada (CU06) no pasa por aca: es una consulta de
 * solo lectura con ranking, en {@link ListaPriorizadaRepository}.
 */
public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    Optional<Postulacion> findByCodigo(UUID codigo);
}
