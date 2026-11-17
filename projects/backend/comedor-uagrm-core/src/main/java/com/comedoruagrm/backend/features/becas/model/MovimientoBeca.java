package com.comedoruagrm.backend.features.becas.model;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.becas.enums.TipoMovimientoBeca;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un cambio del padron (CU23): baja, suspension o reemplazo, con su motivo, responsable y fecha. Es inmutable:
 * la aplicacion solo inserta, por eso no tiene setters.
 *
 * * En un `REEMPLAZO`, `becaId` es la beca liberada y `becaReemplazanteId` la que ocupa el cupo.
 */
@Getter
@Entity
@Table(name = "movimiento_beca")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_movimiento_beca"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimientoBeca extends BaseModel {

    @Column(name = "beca_id", nullable = false, updatable = false)
    private Long becaId;

    @Column(name = "beca_reemplazante_id", updatable = false)
    private Long becaReemplazanteId;

    // Id interno del usuario administrador que registro el movimiento.
    @Column(name = "registrado_por", nullable = false, updatable = false)
    private Long registradoPor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_movimiento_beca", updatable = false)
    private TipoMovimientoBeca tipo;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(name = "motivo", nullable = false, updatable = false, columnDefinition = "text")
    private String motivo;

    public MovimientoBeca(Long becaId, Long becaReemplazanteId, Long registradoPor, TipoMovimientoBeca tipo,
            LocalDate fecha, String motivo) {
        this.becaId = becaId;
        this.becaReemplazanteId = becaReemplazanteId;
        this.registradoPor = registradoPor;
        this.tipo = tipo;
        this.fecha = fecha;
        this.motivo = motivo;
    }
}
