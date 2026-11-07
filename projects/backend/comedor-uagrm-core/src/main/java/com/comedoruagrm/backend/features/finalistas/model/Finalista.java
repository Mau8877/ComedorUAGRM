package com.comedoruagrm.backend.features.finalistas.model;

import java.time.Instant;
import java.time.LocalDate;

import com.comedoruagrm.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Postulacion elegida como finalista de una convocatoria (CU21 la notifica y registra su entrega fisica).
 *
 * * Los finalistas los crea el CU05 (todavia no implementado), que AMPLIA esta tabla sin recrearla.
 * * Solo tienen setter los campos que cambia el CU21: el plazo, cuando se notifico y cuando entrego.
 * * `postulacionId` es un id simple: este modulo no depende de la entidad de postulaciones.
 */
@Getter
@Entity
@Table(name = "finalista")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_finalista"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Finalista extends BaseModel {

    @Column(name = "postulacion_id", nullable = false, updatable = false)
    private Long postulacionId;

    @Column(name = "posicion", nullable = false)
    private int posicion;

    @Setter
    @Column(name = "plazo_entrega")
    private LocalDate plazoEntrega;

    @Setter
    @Column(name = "notificado_en")
    private Instant notificadoEn;

    @Setter
    @Column(name = "documentacion_recibida_en")
    private Instant documentacionRecibidaEn;

    public Finalista(Long postulacionId, int posicion) {
        this.postulacionId = postulacionId;
        this.posicion = posicion;
    }
}
