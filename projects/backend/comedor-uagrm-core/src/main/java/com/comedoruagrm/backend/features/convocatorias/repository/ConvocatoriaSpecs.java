package com.comedoruagrm.backend.features.convocatorias.repository;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;

/**
 * Filtros opcionales del listado de convocatorias (CU13). Se arman como Specification y no como un
 * `@Query` con parametros nulos porque un `UUID` o un enum nulo no tiene tipo para Postgres.
 */
final class ConvocatoriaSpecs {

    private ConvocatoriaSpecs() {
    }

    /** `patron` ya viene en minuscula y con los `%` (null = sin filtro de nombre). */
    static Specification<Convocatoria> filtrar(String patron, EstadoConvocatoria estado, UUID gestionCodigo) {
        Specification<Convocatoria> spec = (root, query, cb) -> cb.conjunction();
        if (patron != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), patron));
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (gestionCodigo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gestion").get("codigo"), gestionCodigo));
        }
        return spec;
    }
}
