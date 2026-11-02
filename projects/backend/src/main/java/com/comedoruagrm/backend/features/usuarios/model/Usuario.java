package com.comedoruagrm.backend.features.usuarios.model;

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
 * Cuenta de acceso del sistema. La contraseña no existe aca: la gestiona
 * Firebase Auth y el enlace es `firebaseUid` (ver SEGURIDAD_AUTH_BACKEND.md).
 *
 * * Solo `activo` tiene setter: activar/desactivar cuentas lo usara CU14.
 */
@Getter
@Entity
@Table(name = "usuario")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_usuario"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "ci", nullable = false, length = 20)
    private String ci;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "firebase_uid", nullable = false, length = 128)
    private String firebaseUid;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Setter
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public Usuario(Rol rol, String ci, String correo, String firebaseUid, String nombres, String apellidos) {
        this.rol = rol;
        this.ci = ci;
        this.correo = correo;
        this.firebaseUid = firebaseUid;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }
}
