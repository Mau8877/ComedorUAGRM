package com.comedoruagrm.backend.features.notificaciones.dto;

import java.util.UUID;

import com.comedoruagrm.backend.features.notificaciones.enums.PlataformaDispositivo;

/** El telefono registrado. No devuelve el token: el cliente ya lo tiene. */
public record DispositivoResponse(UUID codigo, PlataformaDispositivo plataforma) {
}
