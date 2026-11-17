package com.comedoruagrm.backend.features.preseleccion.exception;

/**
 * El modulo de IA no respondio (caido, red, tiempo de espera o error 5xx). Es un fallo interno: nunca llega al
 * cliente, la postulacion queda `pendiente` y el calculo se reintenta (CU02 A7, CU03 A2).
 */
public class IaNoDisponibleException extends RuntimeException {

    public IaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
