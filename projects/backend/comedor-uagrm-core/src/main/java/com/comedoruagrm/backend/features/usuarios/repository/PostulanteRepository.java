package com.comedoruagrm.backend.features.usuarios.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.usuarios.model.Postulante;

public interface PostulanteRepository extends JpaRepository<Postulante, Long> {

    boolean existsByRegistroUniversitario(String registroUniversitario);

    /** El Postulante de una cuenta; vacio si la cuenta es de otro rol. */
    Optional<Postulante> findByUsuarioId(Long usuarioId);
}
