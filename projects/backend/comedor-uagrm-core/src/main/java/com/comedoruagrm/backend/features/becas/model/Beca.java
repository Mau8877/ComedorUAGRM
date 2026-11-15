package com.comedoruagrm.backend.features.becas.model;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.becas.enums.EstadoBeca;

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
 * La beca de un postulante en una convocatoria (CU23). El padron vigente son las becas en estado `VIGENTE`.
 *
 * * Solo cambia de estado: `VIGENTE` pasa a `SUSPENDIDA` o `BAJA`. Una `BAJA` es definitiva.
 * * `adjudicacionId` es nulo en una beca de reemplazo (el reemplazante no tiene adjudicacion propia).
 * * Los ids son simples (este modulo no depende de entidades ajenas): `postulanteId` es el id del usuario.
 */
@Getter
@Entity
@Table(name = "beca")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_beca"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Beca extends BaseModel {

    @Column(name = "convocatoria_id", nullable = false, updatable = false)
    private Long convocatoriaId;

    @Column(name = "postulante_id", nullable = false, updatable = false)
    private Long postulanteId;

    @Column(name = "adjudicacion_id", updatable = false)
    private Long adjudicacionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "estado_beca")
    private EstadoBeca estado;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    public Beca(Long convocatoriaId, Long postulanteId, Long adjudicacionId, LocalDate fechaInicio) {
        this.convocatoriaId = convocatoriaId;
        this.postulanteId = postulanteId;
        this.adjudicacionId = adjudicacionId;
        this.fechaInicio = fechaInicio;
        this.estado = EstadoBeca.VIGENTE;
    }

    /** Una beca vigente pasa a la baja definitiva. */
    public void darDeBaja() {
        this.estado = EstadoBeca.BAJA;
    }

    /** Una beca vigente pasa a suspendida. */
    public void suspender() {
        this.estado = EstadoBeca.SUSPENDIDA;
    }
}
