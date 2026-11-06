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
 * ! Sin comunicado_id ni postulacion_id todavia: las tablas a las que apuntan no existen
 * ! (ver la migracion V20261106100000).
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

    public Notificacion(Usuario usuario, String titulo, String mensaje, TipoNotificacion tipo) {
        this.usuario = usuario;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
    }
}
