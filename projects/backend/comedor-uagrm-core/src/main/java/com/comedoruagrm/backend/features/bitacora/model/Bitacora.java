package com.comedoruagrm.backend.features.bitacora.model;

import java.net.InetAddress;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Evento de auditoria. Append-only: la aplicacion solo inserta, nunca
 * actualiza ni elimina una fila, por eso no tiene setters.
 */
@Getter
@Entity
@Table(name = "bitacora")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_bitacora"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bitacora extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "accion", nullable = false, length = 60)
    private String accion;

    @Column(name = "entidad", length = 60)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle", columnDefinition = "jsonb")
    private Map<String, Object> detalle;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip", columnDefinition = "inet")
    private InetAddress ip;

    public Bitacora(Usuario usuario, String accion, String entidad, Long entidadId,
            Map<String, Object> detalle, InetAddress ip) {
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
        this.ip = ip;
    }
}
