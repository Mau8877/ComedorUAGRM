package com.comedoruagrm.backend.features.postulaciones.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Estado de adjudicacion de una convocatoria (CU05). `cupos`, `puntajeMinimo`, `cuposDisponibles` y
 * `cantidadFinalistas` son nulos mientras no esten definidos. `decisionesHabilitadas` es verdadero solo si
 * la convocatoria esta cerrada, tiene cupos y puntaje minimo y sus resultados no estan publicados.
 * ! `cantidadFinalistas` solo la entrega este endpoint del Administrador.
 */
public record PanelAdjudicacionResponse(
        ConvocatoriaPanel convocatoria,
        Integer cupos,
        BigDecimal puntajeMinimo,
        long adjudicados,
        long noAdjudicados,
        Integer cuposDisponibles,
        Integer cantidadFinalistas,
        long totalFinalistas,
        long calificadasSinSeleccionar,
        boolean resultadosPublicados,
        boolean decisionesHabilitadas) {

    /** Datos basicos de la convocatoria. */
    public record ConvocatoriaPanel(UUID codigo, String nombre, String estado) {
    }
}
