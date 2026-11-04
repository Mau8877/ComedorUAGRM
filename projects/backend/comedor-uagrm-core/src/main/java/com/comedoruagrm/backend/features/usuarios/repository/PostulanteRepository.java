package com.comedoruagrm.backend.features.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.usuarios.model.Postulante;

public interface PostulanteRepository extends JpaRepository<Postulante, Long> {

    boolean existsByRegistroUniversitario(String registroUniversitario);
}
