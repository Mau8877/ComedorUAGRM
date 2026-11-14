package com.comedoruagrm.backend.common.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base para los Models de tablas SIN `id` propio, cuya clave primaria es una
 * FK (ej. `postulante`, cuya PK es `usuario_id`): trae las mismas columnas
 * comunes que {@link BaseModel} (codigo publico, creadoEn, actualizadoEn,
 * borrado logico) pero no declara `id`, que lo define cada Model con
 * `@Id` + `@MapsId`.
 *
 * * Es una clase aparte (con los cuatro campos repetidos) y no una
 * * superclase de BaseModel a proposito: asi BaseModel, que usan todos los
 * * demas Models, queda intacta.
 * * La columna `codigo` se llama `codigo_<tabla>`: cada Model la fija con
 * * @AttributeOverride, igual que con BaseModel.
 */
@Getter
@MappedSuperclass
@SQLRestriction("eliminado_en IS NULL")
public abstract class BaseModelSinId {

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "codigo", updatable = false, nullable = false, unique = true)
    private UUID codigo;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false, nullable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Setter
    @Column(name = "eliminado_en")
    private Instant eliminadoEn;
}
