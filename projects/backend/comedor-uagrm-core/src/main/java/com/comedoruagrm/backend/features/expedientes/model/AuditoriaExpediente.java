package com.comedoruagrm.backend.features.expedientes.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Auditoria del expediente de un finalista (CU07). Es append-only: una auditoria no se edita ni se
 * borra, corregir un resultado es registrar otra; por eso no tiene ningun setter.
 *
 * * La que vale para la adjudicacion es la mas reciente del finalista.
 * * `puntajeAcademico` (0 a 30) lo escribe el auditor y solo es obligatorio si el resultado es VALIDADO;
 * * el puntaje total (IA + academico) no se guarda, se calcula.
 * * `finalistaId` y `auditorId` son ids simples (este modulo no depende de las entidades ajenas).
 */
@Getter
@Entity
@Table(name = "auditoria_expediente")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_auditoria_expediente"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditoriaExpediente extends BaseModel {

    @Column(name = "finalista_id", nullable = false, updatable = false)
    private Long finalistaId;

    @Column(name = "auditor_id", nullable = false, updatable = false)
    private Long auditorId;

    @Column(name = "fecha_auditoria", nullable = false, updatable = false)
    private Instant fechaAuditoria;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "resultado", nullable = false, columnDefinition = "resultado_auditoria", updatable = false)
    private ResultadoAuditoria resultado;

    @Column(name = "observaciones", length = 500, updatable = false)
    private String observaciones;

    @Column(name = "puntaje_academico", precision = 5, scale = 2, updatable = false)
    private BigDecimal puntajeAcademico;

    /** Clave que mando el cliente (header `Idempotency-Key`); nula si la peticion no traia ninguna. */
    @Column(name = "clave_idempotencia", updatable = false)
    private UUID claveIdempotencia;

    public AuditoriaExpediente(Long finalistaId, Long auditorId, Instant fechaAuditoria,
            ResultadoAuditoria resultado, String observaciones, BigDecimal puntajeAcademico) {
        this(finalistaId, auditorId, fechaAuditoria, resultado, observaciones, puntajeAcademico, null);
    }

    public AuditoriaExpediente(Long finalistaId, Long auditorId, Instant fechaAuditoria,
            ResultadoAuditoria resultado, String observaciones, BigDecimal puntajeAcademico,
            UUID claveIdempotencia) {
        this.finalistaId = finalistaId;
        this.auditorId = auditorId;
        this.fechaAuditoria = fechaAuditoria;
        this.resultado = resultado;
        this.observaciones = observaciones;
        this.puntajeAcademico = puntajeAcademico;
        this.claveIdempotencia = claveIdempotencia;
    }
}
