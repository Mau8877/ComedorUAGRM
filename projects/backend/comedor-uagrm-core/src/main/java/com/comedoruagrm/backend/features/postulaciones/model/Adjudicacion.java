package com.comedoruagrm.backend.features.postulaciones.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.postulaciones.enums.ResultadoAdjudicacion;

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
 * Decision final sobre una postulacion: adjudicar o no adjudicar la beca (CU05). Es inmutable: una vez
 * creada no se edita ni se borra, por eso no tiene ningun setter.
 *
 * * `puntajeFinal` es el total (IA + academico) calculado al decidir; no cambia aunque la IA se reprocese.
 * * Puede ser nulo al «no adjudicar» una auditoria sin puntaje academico.
 * * `postulacionId` y `decididoPor` son ids simples (este modulo no depende de entidades ajenas).
 */
@Getter
@Entity
@Table(name = "adjudicacion")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_adjudicacion"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Adjudicacion extends BaseModel {

    @Column(name = "postulacion_id", nullable = false, updatable = false)
    private Long postulacionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "resultado", nullable = false, columnDefinition = "resultado_adjudicacion", updatable = false)
    private ResultadoAdjudicacion resultado;

    @Column(name = "puntaje_final", precision = 5, scale = 2, updatable = false)
    private BigDecimal puntajeFinal;

    // Id interno del usuario administrador que decidio.
    @Column(name = "decidido_por", nullable = false, updatable = false)
    private Long decididoPor;

    @Column(name = "fecha_adjudicacion", nullable = false, updatable = false)
    private Instant fechaAdjudicacion;

    public Adjudicacion(Long postulacionId, ResultadoAdjudicacion resultado, BigDecimal puntajeFinal,
            Long decididoPor, Instant fechaAdjudicacion) {
        this.postulacionId = postulacionId;
        this.resultado = resultado;
        this.puntajeFinal = puntajeFinal;
        this.decididoPor = decididoPor;
        this.fechaAdjudicacion = fechaAdjudicacion;
    }
}
