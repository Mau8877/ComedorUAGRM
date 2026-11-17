package com.comedoruagrm.backend.features.convocatorias.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.service.FechasConvocatoria;

/**
 * Una fila del listado de convocatorias. Las fechas son de calendario (hora de Bolivia).
 *
 * ! `cantidadFinalistas` solo la entrega el Administrador: este DTO solo lo usan los endpoints del CU13.
 */
public record ConvocatoriaResumenResponse(
        UUID codigo,
        String nombre,
        GestionResumenResponse gestion,
        LocalDate fechaApertura,
        LocalDate fechaCierre,
        Integer cupos,
        BigDecimal puntajeMinimo,
        Integer cantidadFinalistas,
        EstadoConvocatoria estado) {

    public static ConvocatoriaResumenResponse de(Convocatoria c) {
        return new ConvocatoriaResumenResponse(c.getCodigo(), c.getNombre(), GestionResumenResponse.de(c.getGestion()),
                FechasConvocatoria.aFecha(c.getFechaApertura()), FechasConvocatoria.aFecha(c.getFechaCierre()),
                c.getCupos(), c.getPuntajeMinimo(), c.getCantidadFinalistas(), c.getEstado());
    }
}
