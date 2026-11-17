package com.comedoruagrm.backend.features.preseleccion.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Reintenta la precalificacion de las postulaciones que siguen `pendiente` (CU02 A7, CU03 A2): las que la IA no
 * pudo calcular y las que se enviaron con el backend apagado. Cada 5 minutos (hora de La Paz); el cron sale de
 * `app.ia.reintento-cron`.
 *
 * * Procesa un lote por pasada, las mas antiguas primero. Si la IA no esta disponible corta la pasada en la
 * * primera: no tiene sentido pegarle 500 veces a un servicio caido.
 */
@Component
@RequiredArgsConstructor
public class PrecalificacionReintentoJob {

    private static final Logger LOG = LoggerFactory.getLogger(PrecalificacionReintentoJob.class);
    static final int LOTE = 500;

    private final PrecalificacionConsultaRepository consulta;
    private final PrecalificacionService precalificacion;

    @Scheduled(cron = "${app.ia.reintento-cron:0 */5 * * * *}", zone = "America/La_Paz")
    public void reintentarPendientes() {
        reintentar();
    }

    /** Devuelve cuantas postulaciones quedaron calificadas en esta pasada. */
    public int reintentar() {
        int calificadas = 0;
        for (Long id : consulta.pendientes(LOTE)) {
            try {
                PrecalificacionService.Resultado resultado = precalificacion.precalificar(id);
                if (resultado == PrecalificacionService.Resultado.CALIFICADA) {
                    calificadas++;
                } else if (resultado == PrecalificacionService.Resultado.IA_NO_DISPONIBLE) {
                    break;
                }
            } catch (RuntimeException e) {
                LOG.error("No se pudo reintentar la precalificacion", kv("module", "preseleccion"),
                        kv("postulacionId", id), kv("error", e.getClass().getSimpleName()));
            }
        }
        if (calificadas > 0) {
            LOG.info("Precalificaciones reintentadas", kv("module", "preseleccion"), kv("calificadas", calificadas));
        }
        return calificadas;
    }
}
