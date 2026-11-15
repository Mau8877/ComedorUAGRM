package com.comedoruagrm.backend.features.convocatorias.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;

public interface GestionAcademicaRepository extends JpaRepository<GestionAcademica, Long> {

    Optional<GestionAcademica> findByCodigo(UUID codigo);

    /** Pagina de gestiones; `busqueda` ya viene en minuscula y con los `%` (null = sin filtro). */
    @Query("""
            select g from GestionAcademica g
            where (:busqueda is null or lower(g.etiqueta) like :busqueda)
            """)
    Page<GestionAcademica> buscar(@Param("busqueda") String busqueda, Pageable pageable);

    boolean existsByEtiqueta(String etiqueta);

    boolean existsByEtiquetaAndIdNot(String etiqueta, Long id);
}
