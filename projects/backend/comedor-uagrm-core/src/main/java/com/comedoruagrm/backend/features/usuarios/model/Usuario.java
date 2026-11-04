package com.comedoruagrm.backend.features.usuarios.model;

import java.time.Instant;

import com.comedoruagrm.backend.common.model.BaseModel;

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
import lombok.Setter;

/**
 * Cuenta de acceso del sistema. Guarda solo el hash BCrypt de la contraseña
 * (nunca en texto plano) y el estado del bloqueo por intentos fallidos
 * (ver SEGURIDAD_AUTH_BACKEND.md).
 *
 * * `activo`, `rol`, `nombres`, `apellidos` y `correo` tienen setter porque la
 * * gestion de cuentas (CU14) los modifica; los tres campos del bloqueo,
 * * porque los maneja el inicio de sesion. El `ci` y el hash NO tienen setter.
 * ! `passwordHash` nunca se loguea ni sale en un DTO.
 */
@Getter
@Entity
@Table(name = "usuario")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario extends BaseModel {

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "ci", nullable = false, length = 20)
    private String ci;

    @Setter
    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Setter
    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Setter
    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Setter
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Setter
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Setter
    @Column(name = "bloqueos_consecutivos", nullable = false)
    private int bloqueosConsecutivos;

    @Setter
    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    public Usuario(Rol rol, String ci, String correo, String passwordHash, String nombres, String apellidos) {
        this.rol = rol;
        this.ci = ci;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }
}
