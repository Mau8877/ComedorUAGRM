package com.comedoruagrm.backend.features.resultados.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.resultados.model.PublicacionResultado;

public interface PublicacionResultadoRepository extends JpaRepository<PublicacionResultado, Long> {

    /** La publicacion con ese codigo publico (el borrado logico lo filtra el modelo). */
    Optional<PublicacionResultado> findByCodigo(UUID codigo);

    boolean existsByConvocatoriaId(long convocatoriaId);
}
