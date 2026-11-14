package com.comedoruagrm.backend.features.notificaciones.model;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.notificaciones.enums.PlataformaDispositivo;
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
 * Telefono donde un usuario recibe push: su token de Firebase.
 *
 * * Un token pertenece a un solo usuario a la vez: si el telefono se comparte, el
 * * registro se reasigna al usuario actual (por eso `usuario` tiene setter).
 * ! El token nunca se loguea ni sale en un DTO.
 */
@Getter
@Entity
@Table(name = "dispositivo_fcm")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_dispositivo_fcm"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DispositivoFcm extends BaseModel {

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", nullable = false, length = 20)
    private PlataformaDispositivo plataforma;

    public DispositivoFcm(Usuario usuario, String token, PlataformaDispositivo plataforma) {
        this.usuario = usuario;
        this.token = token;
        this.plataforma = plataforma;
    }
}
