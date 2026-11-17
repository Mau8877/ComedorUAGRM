package com.comedoruagrm.backend.features.preseleccion.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Al arrancar, un lote que quedo `PENDIENTE` o `EN_PROCESO` con el backend apagado ya no avanza (el reprocesamiento
 * vive en memoria): se marca `FALLIDO` (CU26). Volver a reprocesar toma solo lo que falta.
 *
 * * Supone una sola instancia del backend (la del proyecto); con replicas habria que decidir quien es el dueño de
 * * cada lote.
 */
@Component
@RequiredArgsConstructor
public class ReprocesamientoArranque {

    private static final Logger LOG = LoggerFactory.getLogger(ReprocesamientoArranque.class);

    private final ReprocesamientoConsultaRepository consulta;

    @EventListener(ApplicationReadyEvent.class)
    public void cerrarLotesHuerfanos() {
        try {
            int cerrados = consulta.marcarHuerfanos();
            if (cerrados > 0) {
                LOG.warn("Lotes de reprocesamiento interrumpidos", kv("module", "preseleccion"),
                        kv("lotes", cerrados));
            }
        } catch (RuntimeException e) {
            LOG.error("No se pudieron cerrar los lotes de reprocesamiento huerfanos", kv("module", "preseleccion"),
                    kv("error", e.getClass().getSimpleName()));
        }
    }
}
