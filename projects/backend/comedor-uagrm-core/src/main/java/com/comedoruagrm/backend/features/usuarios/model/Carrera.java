package com.comedoruagrm.backend.features.usuarios.model;

import com.comedoruagrm.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Catalogo de carreras de la UAGRM con su facultad. Solo lectura para la
 * aplicacion: las filas las carga la migracion, por eso no hay constructor
 * publico ni setters.
 */
@Getter
@Entity
@Table(name = "carrera")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_carrera"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Carrera extends BaseModel {

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "facultad", nullable = false, length = 150)
    private String facultad;
}
