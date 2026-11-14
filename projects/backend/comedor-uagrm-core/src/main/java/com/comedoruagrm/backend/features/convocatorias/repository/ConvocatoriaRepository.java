package com.comedoruagrm.backend.features.convocatorias.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;

public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

    Optional<Convocatoria> findByCodigo(UUID codigo);
}
