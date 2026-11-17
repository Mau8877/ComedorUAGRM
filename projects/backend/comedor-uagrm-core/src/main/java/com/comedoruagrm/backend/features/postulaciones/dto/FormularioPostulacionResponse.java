package com.comedoruagrm.backend.features.postulaciones.dto;

import java.util.UUID;

/**
 * Lo que necesita la pantalla «Nueva postulacion» (CU02): la postulacion en edicion (borrador, o devuelta para
 * corregir), su convocatoria, el perfil precargado, las opciones de las listas y, si ya hay, las respuestas.
 */
public record FormularioPostulacionResponse(
        UUID postulacion,
        String estado,
        int numeroEnvios,
        ConvocatoriaFormularioResponse convocatoria,
        PerfilFormularioResponse perfil,
        OpcionesFormularioResponse opciones,
        RespuestaSocioeconomicaResponse respuesta) {
}
