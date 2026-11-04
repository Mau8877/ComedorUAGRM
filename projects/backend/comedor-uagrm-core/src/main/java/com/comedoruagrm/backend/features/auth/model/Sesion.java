package com.comedoruagrm.backend.features.auth.model;

import java.time.Instant;

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
import lombok.Setter;

/**
 * Sesion de un dispositivo: un refresh token. Se guarda solo el hash SHA-256
 * del token, nunca el token. Cerrar sesion (o renovarla) lo revoca.
 */
@Getter
@Entity
@Table(name = "sesion")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_sesion"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sesion extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Setter
    @Column(name = "revocada_en")
    private Instant revocadaEn;

    public Sesion(Usuario usuario, String tokenHash, Instant expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
    }

    public boolean estaVigente(Instant ahora) {
        return revocadaEn == null && expiraEn.isAfter(ahora);
    }
}
