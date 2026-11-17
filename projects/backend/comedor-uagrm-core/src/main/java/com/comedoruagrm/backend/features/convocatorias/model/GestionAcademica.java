package com.comedoruagrm.backend.features.convocatorias.model;

import java.time.LocalDate;

import com.comedoruagrm.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Gestion academica (ej. «2026-II») a la que pertenecen las convocatorias.
 *
 * * La crea y edita el CU13 (alta y edicion de gestiones); el resto de los CU solo la lee.
 */
@Getter
@Entity
@Table(name = "gestion_academica")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_gestion_academica"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GestionAcademica extends BaseModel {

    @Setter
    @Column(name = "etiqueta", nullable = false, length = 50)
    private String etiqueta;

    @Setter
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Setter
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    public GestionAcademica(String etiqueta, LocalDate fechaInicio, LocalDate fechaFin) {
        this.etiqueta = etiqueta;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }
}
