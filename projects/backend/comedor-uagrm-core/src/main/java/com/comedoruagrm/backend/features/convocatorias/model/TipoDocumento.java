package com.comedoruagrm.backend.features.convocatorias.model;

import com.comedoruagrm.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tipo de documento que una convocatoria puede exigir (carnet de identidad, certificado de notas...).
 *
 * * Solo lectura: el CU13 solo lo consulta para elegirlo; el catalogo se administra en otro CU.
 */
@Getter
@Entity
@Table(name = "tipo_documento")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_tipo_documento"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TipoDocumento extends BaseModel {

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
