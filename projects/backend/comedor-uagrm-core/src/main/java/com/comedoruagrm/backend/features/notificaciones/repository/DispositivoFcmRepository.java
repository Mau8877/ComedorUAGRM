package com.comedoruagrm.backend.features.notificaciones.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.notificaciones.model.DispositivoFcm;

public interface DispositivoFcmRepository extends JpaRepository<DispositivoFcm, Long> {

    /** El registro activo de ese token (el borrado logico lo filtra el modelo). */
    Optional<DispositivoFcm> findByToken(String token);

    List<DispositivoFcm> findAllByUsuarioId(Long usuarioId);

    List<DispositivoFcm> findAllByTokenIn(Collection<String> tokens);
}
