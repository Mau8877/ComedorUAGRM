package com.comedoruagrm.backend.features.preseleccion.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Puntaje de vulnerabilidad de una postulacion. Es un historial: solo el `vigente` cuenta para la
 * lista priorizada; los anteriores se conservan (RF-03, RF-21). Solo lectura por ahora.
 */
@Getter
@Entity
@Table(name = "puntaje_postulacion")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_puntaje_postulacion"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PuntajePostulacion extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private ModeloPreseleccion modelo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "postulacion_id", nullable = false)
    private Postulacion postulacion;

    @Column(name = "puntaje_total", nullable = false, precision = 5, scale = 2)
    private BigDecimal puntajeTotal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle")
    private Map<String, Object> detalle;

    @Column(name = "fecha_calculo", nullable = false)
    private Instant fechaCalculo;

    @Column(name = "vigente", nullable = false)
    private boolean vigente;
}
