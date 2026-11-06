package com.comedoruagrm.backend.features.preseleccion.model;

import com.comedoruagrm.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Version del modelo de IA que calcula el puntaje de vulnerabilidad. Solo lectura por ahora:
 * lo administran el CU03 y el CU26.
 */
@Getter
@Entity
@Table(name = "modelo_preseleccion")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_modelo_preseleccion"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModeloPreseleccion extends BaseModel {

    @Column(name = "creado_por")
    private Long creadoPor;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}
