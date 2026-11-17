package com.comedoruagrm.backend.features.cotejos.model;

import java.time.LocalDate;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.cotejos.enums.ResultadoCotejo;

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
 * El resultado de comparar una planilla de asistencia firmada con los registros digitales (CU11): cuantos registros se
 * compararon, cuantos coincidieron, cuantos no, el resultado y las observaciones. Es un registro de lo que se
 * constato: no se edita, por eso no tiene setters.
 *
 * * `planillaId` y `realizadoPor` son ids simples (este modulo no depende de entidades ajenas); `realizadoPor` es el
 * * id interno del usuario administrador.
 * ! `observaciones` es texto libre: nunca viaja a la bitacora ni a un log.
 */
@Getter
@Entity
@Table(name = "cotejo_asistencia")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_cotejo_asistencia"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CotejoAsistencia extends BaseModel {

    @Column(name = "planilla_id", nullable = false, updatable = false)
    private Long planillaId;

    @Column(name = "realizado_por", nullable = false, updatable = false)
    private Long realizadoPor;

    @Column(name = "fecha_cotejo", nullable = false, updatable = false)
    private LocalDate fechaCotejo;

    @Column(name = "total_registros", nullable = false, updatable = false)
    private Integer totalRegistros;

    @Column(name = "coincidencias", nullable = false, updatable = false)
    private Integer coincidencias;

    @Column(name = "discrepancias", nullable = false, updatable = false)
    private Integer discrepancias;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, updatable = false, length = 20)
    private ResultadoCotejo resultado;

    @Column(name = "observaciones", updatable = false, columnDefinition = "text")
    private String observaciones;

    public CotejoAsistencia(Long planillaId, Long realizadoPor, LocalDate fechaCotejo, Integer totalRegistros,
            Integer coincidencias, Integer discrepancias, ResultadoCotejo resultado, String observaciones) {
        this.planillaId = planillaId;
        this.realizadoPor = realizadoPor;
        this.fechaCotejo = fechaCotejo;
        this.totalRegistros = totalRegistros;
        this.coincidencias = coincidencias;
        this.discrepancias = discrepancias;
        this.resultado = resultado;
        this.observaciones = observaciones;
    }
}
