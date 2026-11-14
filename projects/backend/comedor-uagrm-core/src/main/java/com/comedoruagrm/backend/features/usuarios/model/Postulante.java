package com.comedoruagrm.backend.features.usuarios.model;

import com.comedoruagrm.backend.common.model.BaseModelSinId;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de un Postulante. Su clave primaria es la FK a `usuario`
 * (`usuario_id`), por eso no extiende BaseModel sino BaseModelSinId.
 */
@Getter
@Entity
@Table(name = "postulante")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_postulante"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Postulante extends BaseModelSinId {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrera_id", nullable = false)
    private Carrera carrera;

    @Column(name = "registro_universitario", nullable = false, length = 20)
    private String registroUniversitario;

    @Setter
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    public Postulante(Usuario usuario, Carrera carrera, String registroUniversitario, String telefono) {
        this.usuario = usuario;
        this.carrera = carrera;
        this.registroUniversitario = registroUniversitario;
        this.telefono = telefono;
    }
}
