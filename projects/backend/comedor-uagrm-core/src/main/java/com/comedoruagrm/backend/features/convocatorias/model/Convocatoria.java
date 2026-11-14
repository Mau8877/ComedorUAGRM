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

/**
 * Convocatoria de la Beca Comedor de una gestion academica.
 *
 * * Solo lectura por ahora (ver GestionAcademica). `cupos` y `puntaje_minimo` pueden ser nulos
 * * mientras la DUBSS no los defina (CU13).
 */
@Getter
@Entity
@Table(name = "convocatoria")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_convocatoria"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Convocatoria extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestion_id", nullable = false)
    private GestionAcademica gestion;

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "fecha_apertura", nullable = false)
    private Instant fechaApertura;

    @Column(name = "fecha_cierre", nullable = false)
    private Instant fechaCierre;

    @Column(name = "cupos")
    private Integer cupos;

    @Column(name = "puntaje_minimo", precision = 5, scale = 2)
    private BigDecimal puntajeMinimo;

    // Cuantos finalistas se eligen en el CU05 (propuesta por defecto). Solo la ve el Administrador y la
    // escribe el CU13; por eso solo tiene getter.
    @Column(name = "cantidad_finalistas")
    private Integer cantidadFinalistas;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false)
    private EstadoConvocatoria estado;
}
