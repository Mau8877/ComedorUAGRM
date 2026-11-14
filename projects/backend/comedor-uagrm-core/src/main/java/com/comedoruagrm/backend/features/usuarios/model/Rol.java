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
 * Catalogo de roles (POSTULANTE, ADMINISTRADOR_DUBSS, DIRECTIVO,
 * PERSONAL_COMEDOR). Solo lectura para la aplicacion: las filas las siembra
 * la migracion, por eso no hay constructor publico ni setters.
 */
@Getter
@Entity
@Table(name = "rol")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_rol"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rol extends BaseModel {

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;
}
