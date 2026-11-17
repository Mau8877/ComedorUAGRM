package com.comedoruagrm.backend.features.convocatorias.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comedoruagrm.backend.features.convocatorias.model.TipoDocumento;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {

    Optional<TipoDocumento> findByCodigo(UUID codigo);

    List<TipoDocumento> findByCodigoIn(Collection<UUID> codigos);

    /** Pagina del catalogo; `busqueda` ya viene en minuscula y con los `%` (null = sin filtro). */
    @Query("""
            select t from TipoDocumento t
            where (:busqueda is null or lower(t.nombre) like :busqueda)
            """)
    Page<TipoDocumento> buscar(@Param("busqueda") String busqueda, Pageable pageable);
}
