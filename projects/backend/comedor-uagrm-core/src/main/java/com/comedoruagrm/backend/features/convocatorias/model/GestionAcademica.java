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

/**
 * Gestion academica (ej. «2026-II») a la que pertenecen las convocatorias.
 *
 * * Solo lectura por ahora: la lista priorizada (CU06) no la modifica. El CU que cree
 * * gestiones (CU13) agrega el constructor y los setters que necesite.
 */
@Getter
@Entity
@Table(name = "gestion_academica")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_gestion_academica"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GestionAcademica extends BaseModel {

    @Column(name = "etiqueta", nullable = false, length = 50)
    private String etiqueta;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;
}
