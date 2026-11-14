package com.comedoruagrm.backend.features.convocatorias.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;

public interface GestionAcademicaRepository extends JpaRepository<GestionAcademica, Long> {

    Optional<GestionAcademica> findByCodigo(UUID codigo);
}
