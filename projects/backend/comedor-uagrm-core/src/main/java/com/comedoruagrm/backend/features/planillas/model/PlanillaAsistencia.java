package com.comedoruagrm.backend.features.planillas.model;

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

/**
 * Planilla de asistencia imprimible de una convocatoria para un periodo (CU09): el archivo en MinIO, cuantos
 * becados vigentes incluyo y quien la genero. Es un registro de lo que se imprimio: no se edita, por eso no
 * tiene setters.
 *
 * * `convocatoriaId` y `generadaPor` son ids simples (este modulo no depende de entidades ajenas);
 * * `generadaPor` es el id interno del usuario administrador.
 * ! `storagePath` nunca viaja a un response ni a la bitacora.
 */
@Getter
@Entity
@Table(name = "planilla_asistencia")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_planilla_asistencia"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanillaAsistencia extends BaseModel {

    @Column(name = "convocatoria_id", nullable = false, updatable = false)
    private Long convocatoriaId;

    @Column(name = "generada_por", nullable = false, updatable = false)
    private Long generadaPor;

    @Column(name = "fecha_desde", nullable = false, updatable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false, updatable = false)
    private LocalDate fechaHasta;

    @Column(name = "total_becados", nullable = false, updatable = false)
    private Integer totalBecados;

    @Column(name = "storage_path", nullable = false, updatable = false, length = 500)
    private String storagePath;

    @Column(name = "generada_en", nullable = false, updatable = false)
    private Instant generadaEn;

    public PlanillaAsistencia(Long convocatoriaId, Long generadaPor, LocalDate fechaDesde, LocalDate fechaHasta,
            Integer totalBecados, String storagePath, Instant generadaEn) {
        this.convocatoriaId = convocatoriaId;
        this.generadaPor = generadaPor;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.totalBecados = totalBecados;
        this.storagePath = storagePath;
        this.generadaEn = generadaEn;
    }
}
