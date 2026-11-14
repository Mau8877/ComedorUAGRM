package com.comedoruagrm.backend.features.finalistas.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.finalistas.model.Finalista;

/** Acceso a la tabla `finalista`. El listado con estados es una consulta aparte (FinalistaConsultaRepository). */
public interface FinalistaRepository extends JpaRepository<Finalista, Long> {

    Optional<Finalista> findByCodigo(UUID codigo);

    List<Finalista> findByCodigoIn(Collection<UUID> codigos);
}
