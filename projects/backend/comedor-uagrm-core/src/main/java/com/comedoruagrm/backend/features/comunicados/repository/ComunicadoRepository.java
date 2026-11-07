package com.comedoruagrm.backend.features.comunicados.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.comunicados.model.Comunicado;

public interface ComunicadoRepository extends JpaRepository<Comunicado, Long> {

    Optional<Comunicado> findByCodigo(UUID codigo);
}
