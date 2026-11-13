package com.comedoruagrm.backend.features.actas.model;

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
 * Acta de adjudicacion de una convocatoria (CU08): numero, archivo en MinIO y quien la genero. Es definitiva
 * e inmutable (no se regenera ni se elimina), por eso no tiene ningun setter. Hay una sola por convocatoria.
 *
 * * `convocatoriaId` y `generadaPor` son ids simples (este modulo no depende de entidades ajenas);
 * * `generadaPor` es el id interno del usuario administrador.
 * ! `storagePath` nunca viaja a un response ni a la bitacora.
 */
@Getter
@Entity
@Table(name = "acta_adjudicacion")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_acta_adjudicacion"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActaAdjudicacion extends BaseModel {

    @Column(name = "convocatoria_id", nullable = false, updatable = false)
    private Long convocatoriaId;

    @Column(name = "numero", nullable = false, updatable = false, length = 30)
    private String numero;

    @Column(name = "storage_path", nullable = false, updatable = false, length = 500)
    private String storagePath;

    @Column(name = "generada_por", nullable = false, updatable = false)
    private Long generadaPor;

    @Column(name = "generada_en", nullable = false, updatable = false)
    private Instant generadaEn;

    public ActaAdjudicacion(Long convocatoriaId, String numero, String storagePath, Long generadaPor,
            Instant generadaEn) {
        this.convocatoriaId = convocatoriaId;
        this.numero = numero;
        this.storagePath = storagePath;
        this.generadaPor = generadaPor;
        this.generadaEn = generadaEn;
    }
}
