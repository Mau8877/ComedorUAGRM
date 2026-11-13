package com.comedoruagrm.backend.features.notificaciones.service;

import java.util.Map;

/**
 * Lo que recibe el telefono: titulo y cuerpo que muestra el sistema operativo, y datos
 * extra para la app (`codigoNotificacion`, `evento` y, si el aviso tiene postulacion, `postulacionCodigo`).
 */
public record MensajePush(String titulo, String cuerpo, Map<String, String> datos) {
}
