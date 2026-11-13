package com.comedoruagrm.backend.features.dashboard.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Cuando se refresca la vista de indicadores del dashboard (CU12): cada noche a las 02:00 (hora de La Paz, el cron
 * sale de `app.dashboard.refresco-cron`) y, al arrancar, si la vista esta vacia. No hay boton de «actualizar ahora».
 */
@Component
@RequiredArgsConstructor
public class RefrescoIndicadoresJob {

    private final RefrescoIndicadoresService refresco;

    @Scheduled(cron = "${app.dashboard.refresco-cron:0 0 2 * * *}", zone = "America/La_Paz")
    public void refrescarCadaNoche() {
        refresco.refrescar();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        refresco.refrescarSiEstaVacia();
    }
}
