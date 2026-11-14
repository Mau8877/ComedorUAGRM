package com.comedoruagrm.backend.features.expedientes.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.expedientes.model.AuditoriaExpediente;

/** Acceso a la tabla `auditoria_expediente`. La mas reciente de un finalista es la vigente. */
public interface AuditoriaExpedienteRepository extends JpaRepository<AuditoriaExpediente, Long> {

    Optional<AuditoriaExpediente> findFirstByFinalistaIdOrderByFechaAuditoriaDescIdDesc(Long finalistaId);

    Optional<AuditoriaExpediente> findByFinalistaIdAndClaveIdempotencia(Long finalistaId, UUID claveIdempotencia);

    List<AuditoriaExpediente> findByFinalistaIdOrderByFechaAuditoriaDescIdDesc(Long finalistaId);
}
