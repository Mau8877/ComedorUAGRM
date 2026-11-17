package com.comedoruagrm.backend.features.planillas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.planillas.dto.PlanillaResponse;
import com.comedoruagrm.backend.features.planillas.dto.ResumenPlanillaResponse;
import com.comedoruagrm.backend.features.planillas.exception.PlanillasErrorCodes;
import com.comedoruagrm.backend.features.planillas.model.PlanillaAsistencia;
import com.comedoruagrm.backend.features.planillas.repository.PlanillaAsistenciaRepository;
import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository;
import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository.Becado;
import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.planillas.service.PlanillaPdfService.DatosPlanilla;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Generar la planilla de asistencia imprimible de una convocatoria, su vista previa y su resumen (CU09). Solo el
 * Administrador DUBSS.
 *
 * * La planilla refleja el padron vigente al momento de generarla (A4): los cambios posteriores del padron (CU23) solo
 * * aparecen en las planillas que se generen despues. Se puede generar mas de una por convocatoria.
 * * Orden de `generar`: valida el periodo, lee los becados vigentes (sin ninguno no se genera: `ERR_PLA_04`), genera
 * * el PDF, lo sube a MinIO y recien entonces guarda la fila y la bitacora, todo en una transaccion. Si falla el
 * * PDF o MinIO no se guarda nada (A3). Si la base falla despues de subir, el archivo queda huerfano pero nadie lo
 * * enlaza.
 * * La vista previa es el mismo documento marcado «BORRADOR»: no sube, no guarda y no se anota en la bitacora.
 * * No cambia el estado de ninguna beca ni de la convocatoria, y no avisa a nadie.
 * ! Ni el log ni la bitacora llevan nombres, CI, rutas ni direcciones firmadas.
 */
@Service
@RequiredArgsConstructor
public class PlanillaAsistenciaService {

    private static final Logger LOG = LoggerFactory.getLogger(PlanillaAsistenciaService.class);

    private static final String MIME_PDF = "application/pdf";

    private final PlanillasConsultaRepository consulta;
    private final PlanillaAsistenciaRepository planillaRepository;
    private final PlanillaPdfService pdfService;
    private final AlmacenamientoService almacenamiento;
    private final UsuarioService usuarioService;
    private final Clock reloj;

    /** El PDF borrador y el nombre con el que se sugiere abrirlo. */
    public record VistaPreviaPlanilla(String nombreArchivo, byte[] contenido) {
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public ResumenPlanillaResponse resumen(UUID convocatoriaCodigo) {
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        return new ResumenPlanillaResponse(consulta.contarVigentes(c.id()), c.gestion(), c.gestionDesde(),
                c.gestionHasta());
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.GENERACION_PLANILLA, entidad = "planilla_asistencia")
    public ResultadoAuditado<PlanillaResponse> generar(UUID actorCodigo, UUID convocatoriaCodigo, LocalDate desde,
            LocalDate hasta) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        validarPeriodo(c, desde, hasta);
        List<Becado> becados = exigirBecados(c);

        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        String nombreActor = nombre(actor);
        byte[] pdf = pdfService.generar(datos(c, desde, hasta, becados, nombreActor, ahora, false));
        String ruta = ruta(c.codigo(), desde, hasta);
        almacenamiento.subir(ruta, pdf, MIME_PDF);

        PlanillaAsistencia planilla = planillaRepository.saveAndFlush(
                new PlanillaAsistencia(c.id(), actor.getId(), desde, hasta, becados.size(), ruta, ahora));
        LOG.info("Planilla de asistencia generada", kv("module", "planillas"), kv("userId", actorCodigo),
                kv("convocatoria", c.codigo()), kv("becados", becados.size()));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("planilla", planilla.getCodigo().toString());
        detalle.put("convocatoria", c.codigo().toString());
        detalle.put("desde", desde.toString());
        detalle.put("hasta", hasta.toString());
        detalle.put("becados", becados.size());
        PlanillaResponse respuesta = new PlanillaResponse(planilla.getCodigo(), c.codigo(), desde, hasta,
                becados.size(), ahora, nombreActor);
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), planilla.getId(), detalle, true);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public VistaPreviaPlanilla vistaPrevia(UUID convocatoriaCodigo, LocalDate desde, LocalDate hasta) {
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        validarPeriodo(c, desde, hasta);
        List<Becado> becados = exigirBecados(c);
        // Un borrador no es de nadie todavia: «Generada por» queda sin dato.
        byte[] pdf = pdfService.generar(datos(c, desde, hasta, becados, "—", reloj.instant(), true));
        return new VistaPreviaPlanilla("planilla-borrador-" + limpiar(c.nombre()) + ".pdf", pdf);
    }

    private DatosConvocatoria exigirConvocatoria(UUID codigo) {
        return consulta.convocatoria(codigo).orElseThrow(
                () -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));
    }

    /**
     * El periodo es valido si la fecha final no es anterior a la inicial (`ERR_PLA_01`), esta dentro de la gestion
     * academica (`ERR_PLA_02`) y cabe en las columnas de la planilla (`ERR_PLA_03`), en ese orden.
     */
    private static void validarPeriodo(DatosConvocatoria c, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ValidationException(PlanillasErrorCodes.ERR_PLA_01,
                    "La fecha final no puede ser anterior a la inicial");
        }
        if (desde.isBefore(c.gestionDesde()) || hasta.isAfter(c.gestionHasta())) {
            throw new ValidationException(PlanillasErrorCodes.ERR_PLA_02,
                    "El periodo debe estar dentro de la gestión académica " + c.gestion());
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > PlanillaPdfService.MAX_DIAS) {
            throw new ValidationException(PlanillasErrorCodes.ERR_PLA_03,
                    "El periodo no puede superar los " + PlanillaPdfService.MAX_DIAS + " días");
        }
    }

    private List<Becado> exigirBecados(DatosConvocatoria c) {
        List<Becado> becados = consulta.becadosVigentes(c.id());
        if (becados.isEmpty()) {
            throw new ConflictException(PlanillasErrorCodes.ERR_PLA_04,
                    "La convocatoria no tiene becas vigentes: no hay a quién incluir en la planilla");
        }
        return becados;
    }

    private static DatosPlanilla datos(DatosConvocatoria c, LocalDate desde, LocalDate hasta, List<Becado> becados,
            String generadaPor, Instant generadaEn, boolean borrador) {
        return new DatosPlanilla(c.codigo(), c.nombre(), c.gestion(), desde, hasta, becados, generadaPor, generadaEn,
                borrador);
    }

    private static String nombre(Usuario actor) {
        return actor.getNombres() + " " + actor.getApellidos();
    }

    /** `planillas/{codigo_convocatoria}/planilla-{desde}-{hasta}-{aleatorio}.pdf`: dos del mismo periodo no se pisan. */
    private static String ruta(UUID convocatoria, LocalDate desde, LocalDate hasta) {
        return "planillas/" + convocatoria + "/planilla-" + desde + "-" + hasta + "-" + UUID.randomUUID() + ".pdf";
    }

    /** El nombre en minusculas, sin tildes y con guiones, para el nombre de archivo sugerido. */
    private static String limpiar(String texto) {
        String limpio = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return limpio.isEmpty() ? "convocatoria" : limpio;
    }
}
