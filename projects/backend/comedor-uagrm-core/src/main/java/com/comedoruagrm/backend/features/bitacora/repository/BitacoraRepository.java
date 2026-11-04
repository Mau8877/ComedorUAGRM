package com.comedoruagrm.backend.features.bitacora.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.bitacora.model.Bitacora;

public interface BitacoraRepository extends JpaRepository<Bitacora, Long> {
}
