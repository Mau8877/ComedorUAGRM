package com.comedoruagrm.backend.common.storage;

/**
 * Fallo de infraestructura al hablar con el almacenamiento de archivos (MinIO caido, credenciales, red).
 * No es una excepcion de negocio: el `GlobalExceptionHandler` la responde como 500 generico (ERR_SYS_00).
 * Su mensaje nunca lleva la ruta del objeto ni la URL.
 */
public class AlmacenamientoException extends RuntimeException {

    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
