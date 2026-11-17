package com.comedoruagrm.backend.features.convocatorias.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;

public interface ConvocatoriaRepository
        extends JpaRepository<Convocatoria, Long>, JpaSpecificationExecutor<Convocatoria> {

    Optional<Convocatoria> findByCodigo(UUID codigo);

    /** La gestion se trae junto con la pagina: el listado la muestra en cada fila. */
    @Override
    @EntityGraph(attributePaths = "gestion")
    Page<Convocatoria> findAll(Specification<Convocatoria> spec, Pageable pageable);

    /**
     * Pagina del listado del CU13. `patron` ya viene en minuscula y con los `%` (null = sin filtro);
     * `estado` y `gestionCodigo` son opcionales.
     */
    default Page<Convocatoria> buscar(String patron, EstadoConvocatoria estado, UUID gestionCodigo,
            Pageable pageable) {
        return findAll(ConvocatoriaSpecs.filtrar(patron, estado, gestionCodigo), pageable);
    }

    /** Si hay OTRA convocatoria (distinta de `excluirId`) de la gestion en ese estado. */
    boolean existsByGestionIdAndEstadoAndIdNot(Long gestionId, EstadoConvocatoria estado, Long excluirId);

    /** Las convocatorias en ese estado cuyo cierre ya paso (cierre automatico, CU13 A5). */
    List<Convocatoria> findByEstadoAndFechaCierreLessThanEqual(EstadoConvocatoria estado, Instant ahora);
}
