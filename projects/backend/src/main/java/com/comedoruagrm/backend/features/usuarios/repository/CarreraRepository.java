package com.comedoruagrm.backend.features.usuarios.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.usuarios.model.Carrera;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {

    Optional<Carrera> findByCodigo(UUID codigo);
}
