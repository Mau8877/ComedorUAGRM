package com.comedoruagrm.backend.features.preseleccion.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.config.PreseleccionConfig;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.service.CalificacionPostulacionService;
import com.comedoruagrm.backend.features.preseleccion.dto.ModeloAplicable;
import com.comedoruagrm.backend.features.preseleccion.dto.PuntajeCalculado;
import com.comedoruagrm.backend.features.preseleccion.dto.SolicitudPuntaje;
import com.comedoruagrm.backend.features.preseleccion.exception.IaDatosInvalidosException;
import com.comedoruagrm.backend.features.preseleccion.exception.IaNoDisponibleException;
import com.comedoruagrm.backend.features.preseleccion.model.ModeloPreseleccion;
import com.comedoruagrm.backend.features.preseleccion.model.PuntajePostulacion;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository.DatosPuntaje;
import com.comedoruagrm.backend.features.preseleccion.repository.PuntajePostulacionRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Precalificacion de una postulacion con el modulo de IA (CU03), incluida por el CU02 al enviar.
 *
 * * El servicio NO es transaccional: la llamada HTTP a la IA (hasta 5 s) se hace sin transaccion ni conexion
 * * abierta, y solo el registro del resultado va en una transaccion corta. Asi una IA lenta no agota el pool.
 * * Registrar = pasar a historial el puntaje vigente (si hay), insertar el nuevo `vigente` con su fecha de calculo y
 * * la version del modelo, y pasar la postulacion a «calificada», todo junto.
 * * Idempotente (A4): solo calcula una postulacion `pendiente`; despues de calificarse ya no lo es, asi que una
 * * solicitud repetida no genera un segundo puntaje.
 * * Tras {@link #INTENTOS_MAXIMOS} rechazos de la IA (A1) la postulacion se marca para revision y el reintento deja de
 * * tomarla; la IA caida (A2) no cuenta. Reenviar la postulacion reinicia la cuenta.
 * * Si la IA no responde (A2) o rechaza los datos (A1), la postulacion queda `pendiente` y
 * * {@link PrecalificacionReintentoJob} vuelve a intentarlo. Nunca se propaga un error al postulante.
 * * {@link #recalcular} es el calculo del reprocesamiento (CU26): sobre una postulacion `calificada`, con una version
 * * concreta del modelo; conserva el puntaje anterior en el historial y no cambia el estado.
 * ! Se loguea solo el id interno de la postulacion y el tipo de fallo, nunca las respuestas del formulario.
 */
@Service
@RequiredArgsConstructor
public class PrecalificacionService {

    private static final Logger LOG = LoggerFactory.getLogger(PrecalificacionService.class);

    /** Que paso con una solicitud, para el reintento y para las pruebas. */
    public enum Resultado {
        /** Se registro el puntaje y la postulacion quedo «calificada». */
        CALIFICADA,
        /** Se recalculo el puntaje de una postulacion `calificada` con otra version del modelo (CU26). */
        RECALCULADA,
        /** No habia nada que calcular: ya no estaba `pendiente` o le faltan respuestas. */
        OMITIDA,
        /** La IA no esta disponible: queda `pendiente`, se reintenta. */
        IA_NO_DISPONIBLE,
        /** La IA rechazo los datos o devolvio algo inutil, o su version de modelo no esta registrada. */
        NO_CALCULADA
    }

    /** Rechazos de la IA tras los que una postulacion se marca para revision y deja de reintentarse (A1). */
    static final int INTENTOS_MAXIMOS = 3;

    private final PrecalificacionConsultaRepository consulta;
    private final PrecalificacionClient cliente;
    private final ModeloPreseleccionRepository modelos;
    private final ModeloPreseleccionConsultaRepository modelosConsulta;
    private final PuntajePostulacionRepository puntajes;
    private final CalificacionPostulacionService calificacion;
    private final TransactionTemplate transaccion;
    private final EntityManager entityManager;
    private final Clock reloj;

    /** Punto de entrada del CU02 tras confirmar el envio: corre en segundo plano y nunca lanza. */
    @Async(PreseleccionConfig.EJECUTOR_PRECALIFICACION)
    public void solicitar(long postulacionId) {
        try {
            precalificar(postulacionId);
        } catch (RuntimeException e) {
            LOG.warn("La precalificacion fallo", kv("module", "preseleccion"), kv("postulacionId", postulacionId),
                    kv("causa", e.getClass().getSimpleName()));
        }
    }

    /** Calcula y registra el puntaje de una postulacion `pendiente`. */
    public Resultado precalificar(long postulacionId) {
        DatosPuntaje datos = consulta.datosPendiente(postulacionId).orElse(null);
        if (datos == null) {
            return Resultado.OMITIDA;
        }
        ModeloAplicable vigente = modelosConsulta.vigente().map(m -> aplicable(m.id(), m.version())).orElse(null);
        if (vigente != null && vigente.parametros().isEmpty()) {
            // Una version vigente sin parametros (datos anteriores al CU26) no se puede aplicar: mandar `{}` haria que
            // la IA rechace toda postulacion. Se pide el calculo sin version y la IA usa su modelo por defecto.
            LOG.warn("La version vigente del modelo no tiene parametros: se usa el modelo por defecto de la IA",
                    kv("module", "preseleccion"), kv("modelo", vigente.version()));
            vigente = null;
        }
        PuntajeCalculado calculado;
        try {
            calculado = cliente.calcular(solicitud(datos, vigente));
        } catch (IaNoDisponibleException e) {
            LOG.warn("Modulo de IA no disponible: la postulacion sigue pendiente", kv("module", "preseleccion"),
                    kv("postulacionId", postulacionId));
            return Resultado.IA_NO_DISPONIBLE;
        } catch (IaDatosInvalidosException e) {
            LOG.warn("El modulo de IA no pudo calcular el puntaje", kv("module", "preseleccion"),
                    kv("postulacionId", postulacionId), kv("motivo", e.getMessage()));
            registrarFallo(postulacionId);
            return Resultado.NO_CALCULADA;
        }
        return transaccion.execute(estado -> registrar(postulacionId, calculado));
    }

    /** Cuenta el rechazo; al llegar al maximo la marca para revision y avisa (A1). */
    private void registrarFallo(long postulacionId) {
        if (consulta.registrarFallo(postulacionId, INTENTOS_MAXIMOS)) {
            LOG.warn("Postulacion marcada para revision: la IA no puede calificarla", kv("module", "preseleccion"),
                    kv("postulacionId", postulacionId), kv("intentos", INTENTOS_MAXIMOS));
        }
    }

    private Resultado registrar(long postulacionId, PuntajeCalculado calculado) {
        consulta.bloquearPostulacion(postulacionId);
        if (!consulta.sigueEnPendiente(postulacionId)) {
            return Resultado.OMITIDA; // otra solicitud la califico mientras esperabamos a la IA
        }
        ModeloPreseleccion modelo = modelos.findByVersion(calculado.modelo()).orElse(null);
        if (modelo == null) {
            LOG.warn("La version del modelo de IA no esta registrada", kv("module", "preseleccion"),
                    kv("postulacionId", postulacionId), kv("modelo", calculado.modelo()));
            registrarFallo(postulacionId);
            return Resultado.NO_CALCULADA;
        }
        consulta.retirarVigentes(postulacionId);
        puntajes.saveAndFlush(new PuntajePostulacion(modelo,
                entityManager.getReference(Postulacion.class, postulacionId), calculado.puntaje(),
                calculado.detalle(), reloj.instant()));
        calificacion.marcarCalificada(postulacionId);
        LOG.info("Postulacion precalificada", kv("module", "preseleccion"), kv("postulacionId", postulacionId));
        return Resultado.CALIFICADA;
    }

    /**
     * Recalcula una postulacion `calificada` con la version `modelo` (CU26 A3): la IA aplica los parametros que se le
     * mandan, el puntaje vigente pasa a historial y entra el nuevo. La postulacion sigue `calificada`. Una version
     * distinta a la pedida en la respuesta de la IA no sirve (no aplico los parametros): queda `NO_CALCULADA`.
     */
    public Resultado recalcular(long postulacionId, ModeloAplicable modelo) {
        DatosPuntaje datos = consulta.datosCalificada(postulacionId).orElse(null);
        if (datos == null) {
            return Resultado.OMITIDA;
        }
        PuntajeCalculado calculado;
        try {
            calculado = cliente.calcular(solicitud(datos, modelo));
        } catch (IaNoDisponibleException e) {
            LOG.warn("Modulo de IA no disponible al reprocesar", kv("module", "preseleccion"),
                    kv("postulacionId", postulacionId));
            return Resultado.IA_NO_DISPONIBLE;
        } catch (IaDatosInvalidosException e) {
            LOG.warn("El modulo de IA no pudo recalcular el puntaje", kv("module", "preseleccion"),
                    kv("postulacionId", postulacionId), kv("motivo", e.getMessage()));
            return Resultado.NO_CALCULADA;
        }
        if (!modelo.version().equals(calculado.modelo())) {
            LOG.warn("La IA respondio con otra version del modelo", kv("module", "preseleccion"),
                    kv("postulacionId", postulacionId), kv("pedida", modelo.version()),
                    kv("recibida", calculado.modelo()));
            return Resultado.NO_CALCULADA;
        }
        return transaccion.execute(estado -> registrarRecalculo(postulacionId, modelo, calculado));
    }

    private Resultado registrarRecalculo(long postulacionId, ModeloAplicable modelo, PuntajeCalculado calculado) {
        consulta.bloquearPostulacion(postulacionId);
        if (!consulta.sigueCalificada(postulacionId) || consulta.tieneVigenteDelModelo(postulacionId, modelo.id())) {
            return Resultado.OMITIDA; // cambio de estado o ya recalculada mientras esperabamos a la IA
        }
        consulta.retirarVigentes(postulacionId);
        puntajes.saveAndFlush(new PuntajePostulacion(
                entityManager.getReference(ModeloPreseleccion.class, modelo.id()),
                entityManager.getReference(Postulacion.class, postulacionId), calculado.puntaje(),
                calculado.detalle(), reloj.instant()));
        LOG.info("Puntaje recalculado", kv("module", "preseleccion"), kv("postulacionId", postulacionId),
                kv("modelo", modelo.version()));
        return Resultado.RECALCULADA;
    }

    /** La version del modelo y sus ponderaciones, para mandarlas a la IA. */
    public ModeloAplicable aplicable(long modeloId, String version) {
        Map<String, BigDecimal> parametros = new LinkedHashMap<>();
        modelosConsulta.parametros(modeloId).forEach(p -> parametros.put(p.clave(), p.ponderacion()));
        return new ModeloAplicable(modeloId, version, parametros);
    }

    private static SolicitudPuntaje solicitud(DatosPuntaje datos, ModeloAplicable modelo) {
        return new SolicitudPuntaje(datos.postulacion(), datos.ingresosFamiliares(), datos.dependientes(),
                datos.procedencia(), datos.carrera(), datos.patrimonio(), datos.estadoVulnerabilidad(),
                datos.promedioAcademico(), modelo == null ? null : modelo.version(),
                modelo == null ? null : modelo.parametros(), datos.dependenciaEconomica(), datos.ocupacionSosten(),
                datos.tieneHijos(), datos.cantidadHijos(), datos.discapacidad());
    }
}
