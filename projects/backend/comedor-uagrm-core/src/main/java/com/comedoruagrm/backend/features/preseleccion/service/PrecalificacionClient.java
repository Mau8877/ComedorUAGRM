package com.comedoruagrm.backend.features.preseleccion.service;

import com.comedoruagrm.backend.features.preseleccion.dto.PuntajeCalculado;
import com.comedoruagrm.backend.features.preseleccion.dto.SolicitudPuntaje;
import com.comedoruagrm.backend.features.preseleccion.exception.IaDatosInvalidosException;
import com.comedoruagrm.backend.features.preseleccion.exception.IaNoDisponibleException;

/**
 * Puerta al modulo de IA (CU03). Hay una sola implementacion real ({@link IaHttpPrecalificacionClient}); esta
 * interfaz existe para que `PrecalificacionService` se pruebe sin red.
 */
public interface PrecalificacionClient {

    /**
     * @throws IaNoDisponibleException   no respondio a tiempo o respondio con error de servidor
     * @throws IaDatosInvalidosException rechazo los datos o su respuesta no sirve
     */
    PuntajeCalculado calcular(SolicitudPuntaje solicitud);
}
