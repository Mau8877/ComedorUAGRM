package com.comedoruagrm.backend.features.resultados.model;

import java.time.Instant;

import com.comedoruagrm.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Publicacion de los resultados de adjudicacion de una convocatoria (CU22): quien y cuando. Es definitiva e
 * inmutable (no existe despublicar), por eso no tiene ningun setter. Hay una sola por convocatoria.
 *
 * * `convocatoriaId` y `publicadoPor` son ids simples (este modulo no depende de entidades ajenas);
 * * `publicadoPor` es el id interno del usuario administrador.
 */
@Getter
@Entity
@Table(name = "publicacion_resultado")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_publicacion_resultado"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicacionResultado extends BaseModel {

    @Column(name = "convocatoria_id", nullable = false, updatable = false)
    private Long convocatoriaId;

    @Column(name = "publicado_por", nullable = false, updatable = false)
    private Long publicadoPor;

    @Column(name = "publicado_en", nullable = false, updatable = false)
    private Instant publicadoEn;

    public PublicacionResultado(Long convocatoriaId, Long publicadoPor, Instant publicadoEn) {
        this.convocatoriaId = convocatoriaId;
        this.publicadoPor = publicadoPor;
        this.publicadoEn = publicadoEn;
    }
}
