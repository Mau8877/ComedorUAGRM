package com.comedoruagrm.backend.features.postulaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.ComprobantePostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ConvocatoriaFormularioResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios;
import com.comedoruagrm.backend.features.postulaciones.dto.EnviarPostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.FormularioPostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.OpcionesFormularioResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PerfilFormularioResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.RespuestaSocioeconomicaResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.EnvioPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.exception.ValidacionPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.CabeceraEnvio;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.ConvocatoriaAbierta;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.Perfil;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.PostulacionActual;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.Respuesta;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.features.preseleccion.service.PrecalificacionService;

import lombok.RequiredArgsConstructor;

/**
 * Enviar la postulacion a la Beca Comedor (CU02). Solo el Postulante dueño de la postulacion.
 *
 * * Dos operaciones: {@link #iniciar} («Nueva postulacion»: valida con el CU18 y deja la postulacion en
 * * `borrador`, o devuelve la `devuelta` a corregir) y {@link #enviar} (completa y envia).
 * * Enviar, en una sola transaccion con la fila bloqueada: si ya se envio devuelve el mismo comprobante (A6,
 * * idempotente); valida con el CU18 (elegibilidad, duplicados y convocatoria abierta, A4/A8); exige todos los
 * * campos y los documentos obligatorios del CU16 (A3); guarda las respuestas; pasa la postulacion a `pendiente`
 * * (`Postulacion.enviar`) y deja la fila del historial. Recien con el envio confirmado pide la precalificacion
 * * al CU03, en segundo plano: si la IA falla, la postulacion queda `pendiente` y se reintenta (A7).
 * * Los datos del perfil (nombre, CI, carrera) salen de la cuenta, no del cuerpo: no se pueden falsear.
 * ! No se loguean las respuestas del formulario ni datos personales, y no se anota en la bitacora (el cambio de
 * ! estado queda en `postulacion_historial`).
 */
@Service
@RequiredArgsConstructor
public class EnvioPostulacionService {

    private static final Logger LOG = LoggerFactory.getLogger(EnvioPostulacionService.class);
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String MOTIVO_REENVIO = "Reenvío tras corregir";

    private final EnvioPostulacionConsultaRepository consulta;
    private final DocumentosPostulacionConsultaRepository documentos;
    private final ValidacionPostulacionService validacion;
    private final PostulacionRepository postulaciones;
    private final PostulacionHistorialRepository historial;
    private final PrecalificacionService precalificacion;
    private final Clock reloj;

    /**
     * «Nueva postulacion»: crea el borrador de la convocatoria abierta o devuelve el que ya existe (incluida la
     * `devuelta` a corregir, A5). `creada` dice cual de los dos fue.
     */
    public record Inicio(FormularioPostulacionResponse formulario, boolean creada) {
    }

    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional
    public Inicio iniciar(UUID usuarioCodigo) {
        Instant ahora = reloj.instant();
        Perfil perfil = consulta.perfil(usuarioCodigo).orElseThrow(() -> new ForbiddenException(
                ValidacionPostulacionErrorCodes.ERR_VAL_02, "Solo un postulante puede enviar una postulación"));
        ConvocatoriaAbierta convocatoria = consulta.convocatoriaAbierta(ahora).orElseThrow(
                () -> sinConvocatoria(ahora));
        validacion.validar(usuarioCodigo, convocatoria.codigo());

        boolean creada = false;
        PostulacionActual actual = consulta.postulacionDe(perfil.postulanteId(), convocatoria.id()).orElse(null);
        if (actual == null) {
            try {
                actual = consulta.crearBorrador(perfil.postulanteId(), convocatoria.id());
                creada = true;
                LOG.info("Postulacion iniciada", kv("module", "postulaciones"), kv("userId", usuarioCodigo));
            } catch (DataIntegrityViolationException e) {
                // Dos «Nueva postulacion» a la vez: el indice unico dejo pasar una sola.
                throw new ConflictException(ValidacionPostulacionErrorCodes.ERR_VAL_01,
                        "Ya tienes una postulación en curso en esta convocatoria. Vuelve a abrirla");
            }
        }
        return new Inicio(formulario(actual, convocatoria, perfil), creada);
    }

    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional
    public ComprobantePostulacionResponse enviar(UUID usuarioCodigo, UUID postulacionCodigo,
            EnviarPostulacionRequest request) {
        CabeceraEnvio cabecera = consulta.cabecera(postulacionCodigo, usuarioCodigo).orElseThrow(
                () -> new NotFoundException(EnvioPostulacionErrorCodes.ERR_ENV_02, "La postulación no existe"));
        consulta.bloquearPostulacion(cabecera.postulacionId());
        // Se vuelve a leer el estado despues del bloqueo: la peticion que llego segunda ya lo ve cambiado.
        EstadoPostulacion estado = EstadoPostulacion.valueOf(
                consulta.estadoActual(cabecera.postulacionId()).orElseThrow());

        if (estado == EstadoPostulacion.pendiente || estado == EstadoPostulacion.calificada) {
            LOG.info("Envio repetido: se devuelve el comprobante ya emitido", kv("module", "postulaciones"),
                    kv("userId", usuarioCodigo));
            return comprobante(cabecera, consulta.cabecera(postulacionCodigo, usuarioCodigo).orElse(cabecera), false);
        }
        if (estado != EstadoPostulacion.borrador && estado != EstadoPostulacion.devuelta) {
            throw new ConflictException(EnvioPostulacionErrorCodes.ERR_ENV_03,
                    "Esta postulación ya no se puede enviar");
        }

        validacion.validar(usuarioCodigo, cabecera.convocatoriaCodigo());
        RespuestasValidadas validadas = validarRespuestas(request, cabecera);

        boolean reenvio = estado == EstadoPostulacion.devuelta;
        consulta.guardarRespuesta(cabecera.postulacionId(), validadas.respuesta());
        consulta.guardarComplementarios(cabecera.postulacionId(),
                ComplementariosJson.escribir(validadas.complementarios()));
        Postulacion postulacion = postulaciones.findById(cabecera.postulacionId()).orElseThrow();
        postulacion.enviar(reloj.instant());
        postulaciones.saveAndFlush(postulacion);
        historial.saveAndFlush(new PostulacionHistorial(cabecera.postulacionId(), null, estado,
                EstadoPostulacion.pendiente, reenvio ? MOTIVO_REENVIO : null));
        LOG.info("Postulacion enviada", kv("module", "postulaciones"), kv("userId", usuarioCodigo),
                kv("reenvio", reenvio));

        tras(() -> precalificacion.solicitar(cabecera.postulacionId()));
        return new ComprobantePostulacionResponse(cabecera.postulacionCodigo(),
                EstadoPostulacion.pendiente.name(), postulacion.getFechaEnvio(), postulacion.getNumeroEnvios(),
                reenvio, cabecera.convocatoriaNombre(), cabecera.gestion());
    }

    /** El comprobante de una postulacion ya enviada (A6): los mismos datos que la primera vez. */
    private static ComprobantePostulacionResponse comprobante(CabeceraEnvio antes, CabeceraEnvio c,
            boolean reenvio) {
        return new ComprobantePostulacionResponse(c.postulacionCodigo(), c.estado(), c.fechaEnvio(),
                c.numeroEnvios(), reenvio || c.numeroEnvios() > 1, antes.convocatoriaNombre(), antes.gestion());
    }

    /** Lo que se guarda al enviar: las respuestas de columnas y los datos complementarios ya limpios. */
    private record RespuestasValidadas(Respuesta respuesta, DatosComplementarios complementarios) {
    }

    /** Comprueba que este todo lo obligatorio y que cada valor sea valido; devuelve las respuestas a guardar. */
    private RespuestasValidadas validarRespuestas(EnviarPostulacionRequest r, CabeceraEnvio cabecera) {
        List<String> faltantes = new ArrayList<>();
        if (r == null || r.ingresosFamiliares() == null) {
            faltantes.add("ingresos familiares");
        }
        if (r == null || r.dependientes() == null) {
            faltantes.add("número de dependientes");
        }
        if (r == null || esVacio(r.procedencia())) {
            faltantes.add("procedencia");
        }
        if (r == null || esVacio(r.patrimonio())) {
            faltantes.add("patrimonio");
        }
        if (r == null || esVacio(r.estadoVulnerabilidad())) {
            faltantes.add("estado de vulnerabilidad");
        }
        if (r == null || r.promedioAcademico() == null) {
            faltantes.add("promedio académico");
        }
        DatosComplementarios complementarios = ReglasRespuestaSocioeconomica.limpiar(
                r == null ? null : r.complementarios());
        faltantes.addAll(ReglasRespuestaSocioeconomica.faltantesComplementarios(complementarios));
        List<String> documentosFaltantes = documentosObligatoriosFaltantes(cabecera);
        if (!faltantes.isEmpty() || !documentosFaltantes.isEmpty()) {
            throw rechazoFaltantes(faltantes, documentosFaltantes);
        }

        Respuesta respuesta = new Respuesta(r.ingresosFamiliares(), r.dependientes(), r.procedencia().trim(),
                r.patrimonio().trim(), r.estadoVulnerabilidad().trim(), r.promedioAcademico());
        List<String> invalidos = new ArrayList<>(ReglasRespuestaSocioeconomica.invalidos(respuesta));
        invalidos.addAll(ReglasRespuestaSocioeconomica.invalidosComplementarios(complementarios));
        if (!invalidos.isEmpty()) {
            LOG.warn("Envio rechazado: datos invalidos", kv("module", "postulaciones"),
                    kv("errorCode", EnvioPostulacionErrorCodes.ERR_ENV_05));
            throw new ValidationException(EnvioPostulacionErrorCodes.ERR_ENV_05,
                    "Revisa el formulario: " + String.join("; ", invalidos));
        }
        return new RespuestasValidadas(respuesta, complementarios);
    }

    /** Los nombres de los documentos obligatorios de la convocatoria que todavia no tienen ningun archivo. */
    private List<String> documentosObligatoriosFaltantes(CabeceraEnvio cabecera) {
        Set<Long> conArchivo = new HashSet<>();
        documentos.adjuntos(cabecera.postulacionId()).forEach(a -> conArchivo.add(a.tipoDocumentoId()));
        List<String> faltan = new ArrayList<>();
        documentos.requeridos(cabecera.convocatoriaId()).stream()
                .filter(d -> d.obligatorio() && !conArchivo.contains(d.tipoDocumentoId()))
                .forEach(d -> faltan.add(d.nombre()));
        return faltan;
    }

    private ValidationException rechazoFaltantes(List<String> campos, List<String> documentosFaltantes) {
        List<String> partes = new ArrayList<>();
        if (!campos.isEmpty()) {
            partes.add("campos: " + String.join(", ", campos));
        }
        if (!documentosFaltantes.isEmpty()) {
            partes.add("documentos: " + String.join(", ", documentosFaltantes));
        }
        LOG.warn("Envio rechazado: faltan campos o documentos obligatorios", kv("module", "postulaciones"),
                kv("errorCode", EnvioPostulacionErrorCodes.ERR_ENV_04));
        return new ValidationException(EnvioPostulacionErrorCodes.ERR_ENV_04,
                "Falta completar " + String.join(" · ", partes));
    }

    private ConflictException sinConvocatoria(Instant ahora) {
        String proxima = consulta.proximaConvocatoria(ahora).map(p -> " La próxima convocatoria («" + p.nombre()
                + "») abre el " + FECHA.format(p.fechaApertura().atZone(ZONA)) + ".").orElse("");
        LOG.warn("No hay convocatoria abierta", kv("module", "postulaciones"),
                kv("errorCode", EnvioPostulacionErrorCodes.ERR_ENV_01));
        return new ConflictException(EnvioPostulacionErrorCodes.ERR_ENV_01,
                "No hay una convocatoria abierta para postular." + proxima);
    }

    private FormularioPostulacionResponse formulario(PostulacionActual actual, ConvocatoriaAbierta c, Perfil perfil) {
        RespuestaSocioeconomicaResponse respuesta = consulta.respuesta(actual.id())
                .map(r -> new RespuestaSocioeconomicaResponse(r.ingresosFamiliares(), r.dependientes(),
                        r.procedencia(), r.patrimonio(), r.estadoVulnerabilidad(), r.promedioAcademico(),
                        consulta.guardadoEn(actual.id()).orElse(null),
                        ComplementariosJson.leer(consulta.complementarios(actual.id()).orElse(null))))
                .orElse(null);
        return new FormularioPostulacionResponse(actual.codigo(), actual.estado(), actual.numeroEnvios(),
                new ConvocatoriaFormularioResponse(c.codigo(), c.nombre(), c.gestion(), c.fechaCierre()),
                new PerfilFormularioResponse(perfil.nombres(), perfil.apellidos(), perfil.ci(), perfil.carrera()),
                new OpcionesFormularioResponse(OpcionesSocioeconomicas.PROCEDENCIAS,
                        OpcionesSocioeconomicas.PATRIMONIOS, OpcionesSocioeconomicas.ESTADOS_VULNERABILIDAD,
                        OpcionesSocioeconomicas.DEPENDENCIAS, OpcionesSocioeconomicas.OCUPACIONES_SOSTEN,
                        OpcionesSocioeconomicas.RANGOS_INGRESOS, OpcionesSocioeconomicas.CANTIDADES_HIJOS,
                        OpcionesSocioeconomicas.TENENCIAS_VIVIENDA),
                respuesta);
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /** Ejecuta la accion cuando la transaccion se confirma (o ya, si no hay transaccion). */
    private static void tras(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}
