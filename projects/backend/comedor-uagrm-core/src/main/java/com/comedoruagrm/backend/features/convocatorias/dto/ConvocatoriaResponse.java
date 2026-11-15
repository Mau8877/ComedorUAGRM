package com.comedoruagrm.backend.features.convocatorias.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.ConvocatoriaDocumento;
import com.comedoruagrm.backend.features.convocatorias.service.FechasConvocatoria;

/**
 * El detalle de una convocatoria: lo del listado mas sus documentos requeridos y si ya tiene postulaciones
 * (con postulaciones no se cambian la fecha de apertura ni los documentos).
 *
 * ! `cantidadFinalistas` solo la entrega el Administrador: este DTO solo lo usan los endpoints del CU13.
 */
public record ConvocatoriaResponse(
        UUID codigo,
        String nombre,
        GestionResumenResponse gestion,
        LocalDate fechaApertura,
        LocalDate fechaCierre,
        Integer cupos,
        BigDecimal puntajeMinimo,
        Integer cantidadFinalistas,
        EstadoConvocatoria estado,
        boolean tienePostulaciones,
        List<DocumentoRequeridoResponse> documentos) {

    public static ConvocatoriaResponse de(Convocatoria c, List<ConvocatoriaDocumento> documentos,
            boolean tienePostulaciones) {
        return new ConvocatoriaResponse(c.getCodigo(), c.getNombre(), GestionResumenResponse.de(c.getGestion()),
                FechasConvocatoria.aFecha(c.getFechaApertura()), FechasConvocatoria.aFecha(c.getFechaCierre()),
                c.getCupos(), c.getPuntajeMinimo(), c.getCantidadFinalistas(), c.getEstado(), tienePostulaciones,
                documentos.stream().map(DocumentoRequeridoResponse::de).toList());
    }
}
