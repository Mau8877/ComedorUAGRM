package com.comedoruagrm.backend.features.becas.dto;

import java.util.UUID;

/**
 * El resumen del padron de una convocatoria (CU23). `pendientesDeAlta` son los adjudicados que todavia no tienen
 * beca; es 0 mientras los resultados no se publiquen.
 */
public record ResumenPadronResponse(ConvocatoriaPadron convocatoria, boolean resultadosPublicados,
        long adjudicados, long pendientesDeAlta, long vigentes, long suspendidas, long bajas) {

    public record ConvocatoriaPadron(UUID codigo, String nombre) {
    }
}
