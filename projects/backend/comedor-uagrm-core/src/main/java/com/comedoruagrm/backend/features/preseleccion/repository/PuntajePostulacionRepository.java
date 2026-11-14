package com.comedoruagrm.backend.features.preseleccion.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.preseleccion.model.PuntajePostulacion;

public interface PuntajePostulacionRepository extends JpaRepository<PuntajePostulacion, Long> {

    Optional<PuntajePostulacion> findByCodigo(UUID codigo);
}
