package com.comedoruagrm.backend.features.convocatorias.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;

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
import lombok.Setter;

/**
 * Convocatoria de la Beca Comedor de una gestion academica.
 *
 * * La crea y modifica el CU13; el resto de los CU solo la lee. `cupos` y `puntaje_minimo` pueden ser
 * * nulos mientras la DUBSS no los defina. `creadoPor` solo lo fija el constructor.
 */
@Getter
@Entity
@Table(name = "convocatoria")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_convocatoria"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Convocatoria extends BaseModel {

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestion_id", nullable = false)
    private GestionAcademica gestion;

    @Column(name = "creado_por")
    private Long creadoPor;

    @Setter
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Setter
    @Column(name = "fecha_apertura", nullable = false)
    private Instant fechaApertura;

    @Setter
    @Column(name = "fecha_cierre", nullable = false)
    private Instant fechaCierre;

    @Setter
    @Column(name = "cupos")
    private Integer cupos;

    @Setter
    @Column(name = "puntaje_minimo", precision = 5, scale = 2)
    private BigDecimal puntajeMinimo;

    // Cuantos finalistas se eligen en el CU05 (propuesta por defecto). Solo la ve el Administrador y la
    // escribe el CU13.
    @Setter
    @Column(name = "cantidad_finalistas")
    private Integer cantidadFinalistas;

    @Setter
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false)
    private EstadoConvocatoria estado;

    /** Una convocatoria nueva siempre nace en BORRADOR; pasa a ABIERTA y CERRADA con las acciones del CU13. */
    public Convocatoria(GestionAcademica gestion, Long creadoPor, String nombre, Instant fechaApertura,
            Instant fechaCierre, Integer cupos, BigDecimal puntajeMinimo, Integer cantidadFinalistas) {
        this.gestion = gestion;
        this.creadoPor = creadoPor;
        this.nombre = nombre;
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
        this.cupos = cupos;
        this.puntajeMinimo = puntajeMinimo;
        this.cantidadFinalistas = cantidadFinalistas;
        this.estado = EstadoConvocatoria.BORRADOR;
    }
}
