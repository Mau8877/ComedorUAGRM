package com.comedoruagrm.backend.features.asistencias.model;

import java.time.Instant;
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
 * La asistencia de una beca al comedor en un dia (CU10): quien la registro y a que hora. Es un registro de lo que
 * paso: no se edita, por eso no tiene setters.
 *
 * * Una fila por beca y dia (indice unico parcial `uq_asistencia_beca_fecha`). Las filas las crea
 * * `AsistenciaRepository.insertarSiNoExiste`, no `save`, para que repetir la peticion no falle ni duplique.
 * * `becaId` y `registradoPor` son ids simples (este modulo no depende de entidades ajenas); `registradoPor` es el
 * * id interno del usuario del comedor.
 */
@Getter
@Entity
@Table(name = "asistencia")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_asistencia"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asistencia extends BaseModel {

    @Column(name = "beca_id", nullable = false, updatable = false)
    private Long becaId;

    @Column(name = "registrado_por", nullable = false, updatable = false)
    private Long registradoPor;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(name = "hora_registro", nullable = false, updatable = false)
    private Instant horaRegistro;
}
