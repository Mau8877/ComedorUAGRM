package com.comedoruagrm.backend.features.postulaciones.service;

import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;

/**
 * El estado de una postulacion tal como lo ve el postulante (CU04).
 *
 * * La decision final del CU05 (adjudicada o no adjudicada) no se muestra hasta que se publican los resultados
 * * (CU22): antes se ve «calificada» y esa fila del historial se oculta. La devolucion, el archivo y el rechazo
 * * se ven de inmediato.
 * * Funcion pura, sin estado.
 */
public final class EstadoVisible {

    private EstadoVisible() {
    }

    /** El estado a mostrar: «calificada» si la decision final aun no se publico; si no, el real. */
    public static EstadoPostulacion calcular(EstadoPostulacion real, boolean resultadosPublicados) {
        if (!resultadosPublicados && esDecisionFinal(real)) {
            return EstadoPostulacion.calificada;
        }
        return real;
    }

    /** Si la fila del historial con ese estado nuevo debe ocultarse: la decision final sin publicar. */
    public static boolean ocultaFila(EstadoPostulacion estadoNuevo, boolean resultadosPublicados) {
        return !resultadosPublicados && esDecisionFinal(estadoNuevo);
    }

    private static boolean esDecisionFinal(EstadoPostulacion estado) {
        return estado == EstadoPostulacion.adjudicada || estado == EstadoPostulacion.no_adjudicada;
    }
}
