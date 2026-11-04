package com.comedoruagrm.backend.common.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base para todos los Models del proyecto: id interno + codigo (UUID v7)
 * publico, creadoEn, actualizadoEn y borrado logico via eliminadoEn.
 *
 * * `id` (Long, IDENTITY) es la PK real de la tabla -- nunca se expone en
 * * un DTO/URL/token, es un detalle interno de persistencia. `codigo` (UUID
 * * v7, ordenable por tiempo) es el identificador publico (URLs, DTOs), lo
 * * genera Hibernate al insertar y no es la PK. La columna se llama
 * * `codigo_<tabla>` en cada tabla: cada Model la fija con
 * * @AttributeOverride (ver PERSISTENCIA_BD_BACKEND.md#identificador-de-fila-id-interno--codigo_tabla-publico).
 * * El @SQLRestriction filtra "eliminado_en IS NULL" en TODA query que
 * * Hibernate genere para la entidad (findAll, findById, JPQL, fetch de
 * * relaciones) -- es estatico, no se puede "sacar" en una query puntual.
 * * Si algun modulo necesita ver los eliminados (ej. una papelera), se le
 * * agrega un @Filter opcional a esa entidad puntual, no se cambia este
 * * mecanismo global.
 * * Getters generados por Lombok (@Getter a nivel de clase). `eliminadoEn`
 * * es el unico campo con setter (borrado logico) -- id/codigo/creadoEn/
 * * actualizadoEn son de solo lectura desde fuera de esta clase.
 * ! Las tablas cuya clave es una FK o compuesta (postulante,
 * ! respuesta_socioeconomica, convocatoria_documento) no extienden esta
 * ! clase tal cual: ver la nota de PERSISTENCIA_BD_BACKEND.md.
 */
@Getter
@MappedSuperclass
@SQLRestriction("eliminado_en IS NULL")
public abstract class BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

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
