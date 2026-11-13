package com.comedoruagrm.backend.features.actas.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.actas.model.ActaAdjudicacion;

public interface ActaAdjudicacionRepository extends JpaRepository<ActaAdjudicacion, Long> {

    /** El acta con ese codigo publico (el borrado logico lo filtra el modelo). */
    Optional<ActaAdjudicacion> findByCodigo(UUID codigo);

    Optional<ActaAdjudicacion> findByConvocatoriaId(long convocatoriaId);

    boolean existsByConvocatoriaId(long convocatoriaId);
}
