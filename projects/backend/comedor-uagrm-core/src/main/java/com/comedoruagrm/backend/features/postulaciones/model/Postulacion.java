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
 * * La lista priorizada (CU06) no cambia su estado. El CU19 la puede devolver (`devolver()`) y el CU20 archivar o rechazar (`archivar()`/`rechazar()`) y el CU05 adjudicarla o no adjudicarla (`adjudicar()`/`noAdjudicar()`); el
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

    /**
     * Pasa la postulacion de «calificada» a «archivada» (CU20). Estado final: no hay vuelta atras. La regla
     * de negocio con su codigo de error es del `service`; esto es solo la red de seguridad del Model.
     */
    public void archivar() {
        cerrar(EstadoPostulacion.archivada);
    }

    /** Pasa la postulacion de «calificada» a «no adjudicada» (CU20, rechazo). Estado final. */
    public void rechazar() {
        cerrar(EstadoPostulacion.no_adjudicada);
    }

    /** Pasa la postulacion de «calificada» a «adjudicada» (CU05). Estado final. */
    public void adjudicar() {
        decidir(EstadoPostulacion.adjudicada);
    }

    /** Pasa la postulacion de «calificada» a «no adjudicada» por decision final (CU05). Estado final. */
    public void noAdjudicar() {
        decidir(EstadoPostulacion.no_adjudicada);
    }

    private void decidir(EstadoPostulacion nuevo) {
        if (estado != EstadoPostulacion.calificada) {
            throw new IllegalStateException("Solo se puede decidir una postulacion calificada");
        }
        this.estado = nuevo;
    }

    private void cerrar(EstadoPostulacion nuevo) {
        if (estado != EstadoPostulacion.calificada) {
            throw new IllegalStateException("Solo se puede cerrar una postulacion calificada");
        }
        this.estado = nuevo;
    }
}
