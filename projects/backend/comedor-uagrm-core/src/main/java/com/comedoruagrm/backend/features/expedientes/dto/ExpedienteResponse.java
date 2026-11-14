package com.comedoruagrm.backend.features.expedientes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Expediente completo de un finalista (CU07): lo que el postulante declaro y adjunto, el puntaje vigente de
 * la IA (0 a 70), la entrega fisica y las auditorias de la mas reciente a la mas antigua.
 *
 * * `puntajeAcademico` es el de la ULTIMA auditoria y `puntajeTotal` = `puntajeIa` + `puntajeAcademico`:
 * * es null si no hay auditoria, si la ultima no tiene puntaje academico o si no hay puntaje de la IA.
 * * El total no se guarda, se calcula en cada lectura.
 * * `desglose` es el `detalle` que guarda el modulo de IA (null si no existe).
 * ! Solo codigos publicos: nunca ids internos ni la ruta de almacenamiento de un documento.
 */
public record ExpedienteResponse(
        FinalistaExpediente finalista,
        PostulanteExpediente postulante,
        ConvocatoriaExpediente convocatoria,
        RespuestaSocioeconomicaResponse respuestaSocioeconomica,
        List<DocumentoExpedienteResponse> documentos,
        String estadoPostulacion,
        BigDecimal puntajeIa,
        Map<String, Object> desglose,
        BigDecimal puntajeAcademico,
        BigDecimal puntajeTotal,
        List<AuditoriaResponse> auditorias) {

    /** El finalista y el estado de su entrega fisica (`entrega` es RECIBIDA o PENDIENTE). */
    public record FinalistaExpediente(
            UUID codigo,
            int posicion,
            LocalDate plazoEntrega,
            EntregaExpediente entrega) {
    }

    public record EntregaExpediente(String estado, LocalDate fecha) {
    }

    public record PostulanteExpediente(String nombres, String apellidos, String ci, String carrera) {
    }

    public record ConvocatoriaExpediente(UUID codigo, String nombre) {
    }

    /** Un documento digital: `estado` es RECIBIDO o FALTA; sin adjunto, `codigo`, `nombreArchivo` y `mimeType` son null. */
    public record DocumentoExpedienteResponse(
            UUID codigo,
            String tipo,
            boolean obligatorio,
            String estado,
            String nombreArchivo,
            String mimeType,
            Instant subidoEn) {
    }

    public record RespuestaSocioeconomicaResponse(
            BigDecimal ingresosFamiliares,
            Short dependientes,
            String patrimonio,
            String procedencia,
            BigDecimal promedioAcademico,
            String estadoVulnerabilidad) {
    }
}
