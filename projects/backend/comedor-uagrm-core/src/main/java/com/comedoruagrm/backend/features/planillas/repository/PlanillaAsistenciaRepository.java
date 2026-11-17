package com.comedoruagrm.backend.features.planillas.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.planillas.model.PlanillaAsistencia;

public interface PlanillaAsistenciaRepository extends JpaRepository<PlanillaAsistencia, Long> {

    /** La planilla con ese codigo publico (el borrado logico lo filtra el modelo). */
    Optional<PlanillaAsistencia> findByCodigo(UUID codigo);
}
