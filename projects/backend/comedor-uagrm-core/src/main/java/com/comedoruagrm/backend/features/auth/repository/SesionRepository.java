package com.comedoruagrm.backend.features.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.auth.model.Sesion;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByTokenHash(String tokenHash);
}
