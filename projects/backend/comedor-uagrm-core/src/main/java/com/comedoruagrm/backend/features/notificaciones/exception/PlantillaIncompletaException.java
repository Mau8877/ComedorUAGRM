package com.comedoruagrm.backend.features.notificaciones.exception;

/**
 * La plantilla de un evento necesita un dato que quien notifico no paso. Es un error de
 * programacion de quien llama, no una falla de negocio: no llega al cliente, se registra y el
 * aviso no se guarda ni se envia.
 *
 * ! El mensaje nombra la clave que falta, nunca los valores de los datos.
 */
public class PlantillaIncompletaException extends RuntimeException {

    public PlantillaIncompletaException(String evento, String clave) {
        super("Falta el dato '" + clave + "' para la plantilla del evento " + evento);
    }
}
