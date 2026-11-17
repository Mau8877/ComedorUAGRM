package com.comedoruagrm.backend.features.preseleccion.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.preseleccion.model.ModeloPreseleccion;

public interface ModeloPreseleccionRepository extends JpaRepository<ModeloPreseleccion, Long> {

    Optional<ModeloPreseleccion> findByCodigo(UUID codigo);

    /** La version que informo el modulo de IA al calcular un puntaje (CU03). */
    Optional<ModeloPreseleccion> findByVersion(String version);
}
