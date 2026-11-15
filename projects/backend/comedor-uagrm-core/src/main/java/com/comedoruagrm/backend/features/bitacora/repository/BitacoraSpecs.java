package com.comedoruagrm.backend.features.bitacora.repository;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import com.comedoruagrm.backend.features.bitacora.model.Bitacora;

import jakarta.persistence.criteria.JoinType;

/** Filtros opcionales de la consulta de la bitacora (CU25), todos combinados con AND. */
final class BitacoraSpecs {

    private BitacoraSpecs() {
    }

    /**
     * `patron` ya viene en minuscula y con los `%` (busca en nombres, apellidos y correo del usuario); `desde`
     * es inclusivo y `hasta` exclusivo. Cualquiera en null = sin ese filtro.
     */
    static Specification<Bitacora> filtrar(String patron, String accion, Instant desde, Instant hasta) {
        Specification<Bitacora> spec = (root, query, cb) -> cb.conjunction();
        if (patron != null) {
            spec = spec.and((root, query, cb) -> {
                var usuario = root.join("usuario", JoinType.INNER);
                return cb.or(cb.like(cb.lower(usuario.get("nombres")), patron),
                        cb.like(cb.lower(usuario.get("apellidos")), patron),
                        cb.like(cb.lower(usuario.get("correo")), patron));
            });
        }
        if (accion != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("accion"), accion));
        }
        if (desde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("creadoEn"), desde));
        }
        if (hasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("creadoEn"), hasta));
        }
        return spec;
    }
}
