package com.comedoruagrm.backend.features.notificaciones.model;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;

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
import lombok.Setter;

/**
 * Aviso de la bandeja de un usuario. Una fila por aviso, se envie por la via que se envie.
 *
 * * `leida` y `enviadaEn` tienen setter: los cambia la bandeja y el despacho.
 * * `postulacionId` y `comunicadoId` son el contexto opcional del aviso (CU21): el aviso a un
 * * finalista lleva su postulacion y el comunicado publicado lleva el suyo. Son ids simples y no
 * * relaciones: este modulo no depende de las entidades de postulaciones ni de comunicados.
 */
@Getter
@Entity
@Table(name = "notificacion")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_notificacion"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notificacion extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "text")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_notificacion")
    private TipoNotificacion tipo;

    @Setter
    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Setter
    @Column(name = "enviada_en")
    private Instant enviadaEn;

    @Column(name = "postulacion_id")
    private Long postulacionId;

    @Column(name = "comunicado_id")
    private Long comunicadoId;

    public Notificacion(Usuario usuario, String titulo, String mensaje, TipoNotificacion tipo) {
        this(usuario, titulo, mensaje, tipo, null, null);
    }

    public Notificacion(Usuario usuario, String titulo, String mensaje, TipoNotificacion tipo,
            Long postulacionId, Long comunicadoId) {
        this.usuario = usuario;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.postulacionId = postulacionId;
        this.comunicadoId = comunicadoId;
    }
}
