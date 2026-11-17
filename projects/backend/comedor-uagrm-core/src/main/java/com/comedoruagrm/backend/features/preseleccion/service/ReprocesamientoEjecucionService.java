package com.comedoruagrm.backend.features.preseleccion.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import com.comedoruagrm.backend.config.PreseleccionConfig;
import com.comedoruagrm.backend.features.preseleccion.dto.ModeloAplicable;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository.LoteFila;
import com.comedoruagrm.backend.features.preseleccion.service.PrecalificacionService.Resultado;

import lombok.RequiredArgsConstructor;

/**
 * Ejecuta en segundo plano un lote de reprocesamiento (CU26): recalcula, una por una, las postulaciones `calificada`
 * de la convocatoria que no tienen puntaje vigente de la version del lote.
 *
 * * Corre en el ejecutor `ejecutorPrecalificacion` y NO es transaccional: cada postulacion se recalcula en su propia
 * * transaccion corta ({@link PrecalificacionService#recalcular}) y el avance se escribe aparte, asi el cliente que
 * * consulta el lote ve el progreso real.
 * * Si falla una postulacion se cuenta en `fallidas` y se sigue (A5). Si la IA no esta disponible se corta el lote
 * * como `FALLIDO`: no tiene sentido pegarle a un servicio caido por cada postulacion. Un lote nuevo retoma solo las
 * * que faltan.
 * ! Nunca se loguean las respuestas del formulario, solo ids internos y tipos de fallo.
 */
@Service
@RequiredArgsConstructor
public class ReprocesamientoEjecucionService {

    private static final Logger LOG = LoggerFactory.getLogger(ReprocesamientoEjecucionService.class);

    private final ReprocesamientoConsultaRepository consulta;
    private final PrecalificacionService precalificacion;

    /** Arranca el lote cuando la transaccion que lo creo ya confirmo. */
    @Async(PreseleccionConfig.EJECUTOR_PRECALIFICACION)
    @TransactionalEventListener
    public void alCrearseElLote(LoteReprocesamientoCreado evento) {
        ejecutar(evento.loteId());
    }

    /** Procesa el lote hasta el final. Nunca lanza: un fallo inesperado deja el lote `FALLIDO`. */
    public void ejecutar(long loteId) {
        LoteFila lote = consulta.loteDe(loteId).orElse(null);
        if (lote == null || !"PENDIENTE".equals(lote.estado())) {
            return;
        }
        try {
            ModeloAplicable modelo = precalificacion.aplicable(lote.modeloId(), lote.modeloVersion());
            List<Long> ids = consulta.porReprocesar(lote.convocatoriaId(), lote.modeloId());
            consulta.marcarEnProceso(loteId, ids.size());
            boolean cortado = false;
            for (Long id : ids) {
                boolean fallida;
                try {
                    Resultado resultado = precalificacion.recalcular(id, modelo);
                    if (resultado == Resultado.IA_NO_DISPONIBLE) {
                        cortado = true;
                        break;
                    }
                    fallida = resultado == Resultado.NO_CALCULADA;
                } catch (RuntimeException e) {
                    LOG.error("No se pudo recalcular una postulacion", kv("module", "preseleccion"),
                            kv("loteId", loteId), kv("postulacionId", id), kv("error", e.getClass().getSimpleName()));
                    fallida = true;
                }
                consulta.sumarAvance(loteId, fallida);
            }
            consulta.terminar(loteId, cortado ? "FALLIDO" : "COMPLETADO");
            LOG.info("Reprocesamiento terminado", kv("module", "preseleccion"), kv("loteId", loteId),
                    kv("estado", cortado ? "FALLIDO" : "COMPLETADO"));
        } catch (RuntimeException e) {
            LOG.error("El reprocesamiento fallo", kv("module", "preseleccion"), kv("loteId", loteId),
                    kv("error", e.getClass().getSimpleName()));
            try {
                consulta.terminar(loteId, "FALLIDO");
            } catch (RuntimeException ignorada) {
                LOG.error("No se pudo marcar el lote como fallido", kv("module", "preseleccion"),
                        kv("loteId", loteId));
            }
        }
    }
}
