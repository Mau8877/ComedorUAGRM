package com.comedoruagrm.backend.features.postulaciones.model;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Postulacion de un Postulante a una convocatoria.
 *
 * * La lista priorizada (CU06) no cambia su estado. El CU19 solo la puede devolver (`devolver()`); el
 * * CU que la crea (CU02) agrega el constructor y los setters que necesite.
 */
@Getter
@Entity
@Table(name = "postulacion")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_postulacion"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Postulacion extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convocatoria_id", nullable = false)
    private Convocatoria convocatoria;

    // `usuario_id` del postulante (la PK de `postulante` es la FK a `usuario`).
    @Column(name = "postulante_id", nullable = false)
    private Long postulanteId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false)
    private EstadoPostulacion estado;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio;

    @Column(name = "numero_envios", nullable = false)
    private short numeroEnvios;

    /**
     * Pasa la postulacion de «calificada» a «devuelta» (CU19). La regla de negocio con su codigo de
     * error es del `service`; esto es solo la red de seguridad del Model.
     */
    public void devolver() {
        if (estado != EstadoPostulacion.calificada) {
            throw new IllegalStateException("Solo se puede devolver una postulacion calificada");
        }
        this.estado = EstadoPostulacion.devuelta;
    }
}
