package com.comedoruagrm.backend.features.notificaciones.service;

import java.util.List;

/**
 * Que paso al enviar un push a varios telefonos.
 *
 * * `tokensInvalidos`: Firebase dice que el token ya no sirve (app desinstalada): hay que
 * * eliminarlo. `tokensPasajeros`: fallaron por un problema pasajero (Firebase caido, cuota):
 * * vale la pena reintentarlos. Un token que fallo por otra causa no esta en ninguna lista
 * * y solo se cuenta como no entregado.
 */
public record ResultadoPush(int enviados, List<String> tokensInvalidos, List<String> tokensPasajeros) {

    public static ResultadoPush vacio() {
        return new ResultadoPush(0, List.of(), List.of());
    }
}
