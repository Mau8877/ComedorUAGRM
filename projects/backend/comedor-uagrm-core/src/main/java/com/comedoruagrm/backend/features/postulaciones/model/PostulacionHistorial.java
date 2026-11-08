package com.comedoruagrm.backend.features.postulaciones.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un cambio de estado de una postulacion (CU19: devolver; lo usan tambien CU02, CU20 y CU05). Es
 * append-only: una fila no se edita ni se borra, por eso no tiene ningun setter.
 *
 * * `postulacionId` y `usuarioId` son ids simples (este modulo no depende de las entidades ajenas).
 * * `usuarioId` es nulo si el cambio lo hizo el sistema.
 */
@Getter
@Entity
@Table(name = "postulacion_historial")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_postulacion_historial"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostulacionHistorial extends BaseModel {

    @Column(name = "postulacion_id", nullable = false, updatable = false)
    private Long postulacionId;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_anterior", nullable = false, columnDefinition = "estado_postulacion", updatable = false)
    private EstadoPostulacion estadoAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_nuevo", nullable = false, columnDefinition = "estado_postulacion", updatable = false)
    private EstadoPostulacion estadoNuevo;

    @Column(name = "motivo", columnDefinition = "text", updatable = false)
    private String motivo;

    public PostulacionHistorial(Long postulacionId, Long usuarioId, EstadoPostulacion estadoAnterior,
            EstadoPostulacion estadoNuevo, String motivo) {
        this.postulacionId = postulacionId;
        this.usuarioId = usuarioId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
    }
}
