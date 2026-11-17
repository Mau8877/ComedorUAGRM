package com.comedoruagrm.backend.features.bitacora.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.comedoruagrm.backend.features.bitacora.model.Bitacora;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long>, JpaSpecificationExecutor<Bitacora> {

    /** El usuario se trae junto con la pagina: cada fila lo muestra. */
    @Override
    @EntityGraph(attributePaths = "usuario")
    Page<Bitacora> findAll(Specification<Bitacora> spec, Pageable pageable);

    /** Pagina de la consulta del CU25 (ver {@link BitacoraSpecs#filtrar}). */
    default Page<Bitacora> buscar(String patron, String accion, Instant desde, Instant hasta, Pageable pageable) {
        return findAll(BitacoraSpecs.filtrar(patron, accion, desde, hasta), pageable);
    }

    /** Las acciones que ya aparecen en la bitacora, para llenar el filtro de acciones. */
    @Query("SELECT DISTINCT b.accion FROM Bitacora b ORDER BY b.accion")
    List<String> accionesRegistradas();
}
