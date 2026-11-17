package com.comedoruagrm.backend.features.preseleccion.exception;

/** El modulo de IA rechazo los datos (4xx) o devolvio una respuesta que no se puede usar (CU03 A1). */
public class IaDatosInvalidosException extends RuntimeException {

    public IaDatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
