package com.comedoruagrm.backend.features.notificaciones.service;

import java.util.List;

/**
 * Canal por el que sale un push. Dos implementaciones: {@link CanalPushFcm} (Firebase) cuando
 * hay credenciales y {@link CanalPushLog} (solo escribe en el log) cuando no; los tests y el
 * CI usan la segunda, nunca llaman a Firebase.
 */
public interface CanalPush {

    /** Envia el mismo mensaje a todos los tokens y dice que paso con cada uno. Nunca lanza. */
    ResultadoPush enviar(List<String> tokens, MensajePush mensaje);
}
