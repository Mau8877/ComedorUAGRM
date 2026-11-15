package com.comedoruagrm.backend.features.convocatorias.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Cierra solas las convocatorias ABIERTA cuya fecha de cierre ya paso (CU13, A5): cada 5 minutos (hora de La
 * Paz, el cron sale de `app.convocatorias.cierre-cron`) y, al arrancar, para recuperar lo que vencio con el
 * backend apagado. No hay boton de «cerrar vencidas ahora».
 *
 * * Cierra una por una, cada una en su transaccion: el fallo de una no frena a las demas.
 */
@Component
@RequiredArgsConstructor
public class CierreAutomaticoConvocatoriasJob {

    private static final Logger LOG = LoggerFactory.getLogger(CierreAutomaticoConvocatoriasJob.class);

    private final ConvocatoriaRepository convocatoriaRepository;
    private final ConvocatoriaEstadoService estadoService;

    @Scheduled(cron = "${app.convocatorias.cierre-cron:0 */5 * * * *}", zone = "America/La_Paz")
    public void cerrarCadaCincoMinutos() {
        cerrarVencidas();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        cerrarVencidas();
    }

    /** Devuelve cuantas convocatorias cerro. */
    public int cerrarVencidas() {
        int cerradas = 0;
        for (Convocatoria vencida : convocatoriaRepository
                .findByEstadoAndFechaCierreLessThanEqual(EstadoConvocatoria.ABIERTA, Instant.now())) {
            try {
                if (estadoService.cerrarVencida(vencida.getId()).auditar()) {
                    cerradas++;
                }
            } catch (RuntimeException e) {
                // Sin datos personales: solo el codigo publico y el tipo de error.
                LOG.error("No se pudo cerrar la convocatoria vencida", kv("module", "convocatorias"),
                        kv("convocatoriaId", vencida.getCodigo()), kv("error", e.getClass().getSimpleName()));
            }
        }
        return cerradas;
    }
}
