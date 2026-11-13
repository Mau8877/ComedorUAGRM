package com.comedoruagrm.backend.features.actas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.actas.dto.ActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaActaResponse;
import com.comedoruagrm.backend.features.actas.exception.ActasErrorCodes;
import com.comedoruagrm.backend.features.actas.model.ActaAdjudicacion;
import com.comedoruagrm.backend.features.actas.repository.ActaAdjudicacionRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.Adjudicado;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.actas.service.ActaPdfService.DatosActa;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Generar el acta de adjudicacion de una convocatoria y su vista previa (CU08). Solo el Administrador DUBSS.
 *
 * * Una sola acta por convocatoria, definitiva: no se regenera ni se elimina. Solo se genera con los resultados
 * * publicados (CU22); publicar no exige el acta.
 * * Orden de `generar`: valida, bloquea la convocatoria (`FOR UPDATE`), asigna el numero, genera el PDF, lo sube a
 * * MinIO y recien entonces guarda la fila y la bitacora, todo en una transaccion. Si falla el PDF o MinIO no se
 * * guarda nada. Si la base falla despues de subir, el archivo queda huerfano pero nadie lo enlaza.
 * * No hay idempotencia por clave: la segunda peticion de la misma convocatoria recibe `ERR_ACT_02`; la unicidad
 * * de `convocatoria_id` es la segunda red de seguridad.
 * * La vista previa es el mismo documento con el numero «borrador»: no sube, no guarda, no consume numero y no
 * * se anota en la bitacora.
 * * No cambia el estado de ninguna postulacion, adjudicacion ni de la convocatoria, y no avisa a nadie.
 * ! Ni el log ni la bitacora llevan nombres, CI, rutas ni direcciones firmadas.
 */
@Service
@RequiredArgsConstructor
public class ActaAdjudicacionService {

    private static final Logger LOG = LoggerFactory.getLogger(ActaAdjudicacionService.class);

    private static final String MIME_PDF = "application/pdf";
    private static final String INDICE_UNICO_CONVOCATORIA = "uq_acta_adjudicacion_convocatoria";

    private final ActasConsultaRepository consulta;
    private final ActaAdjudicacionRepository actaRepository;
    private final NumeroActaService numeroActaService;
    private final ActaPdfService pdfService;
    private final AlmacenamientoService almacenamiento;
    private final UsuarioService usuarioService;
    private final Clock reloj;

    /** El PDF borrador y el nombre con el que se sugiere abrirlo. */
    public record VistaPreviaActa(String nombreArchivo, byte[] contenido) {
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.GENERACION_ACTA, entidad = "acta_adjudicacion")
    public ResultadoAuditado<ActaResponse> generar(UUID actorCodigo, UUID convocatoriaCodigo) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        DatosConvocatoria buscada = exigirConvocatoria(convocatoriaCodigo);

        // Primero el bloqueo y despues se vuelve a leer todo: la peticion que llego segunda ya ve el acta.
        consulta.bloquearConvocatoria(buscada.id());
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        validar(c);

        List<Adjudicado> adjudicados = consulta.adjudicadosDelActa(c.id());
        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        String numero = numeroActaService.siguiente(ahora);
        String nombreActor = nombre(actor);
        byte[] pdf = pdfService.generar(datos(c, adjudicados, nombreActor, ahora), numero);
        String ruta = ruta(c.codigo(), numero);
        almacenamiento.subir(ruta, pdf, MIME_PDF);

        ActaAdjudicacion acta = guardar(c, numero, ruta, actor, ahora);
        LOG.info("Acta de adjudicación generada", kv("module", "actas"), kv("userId", actorCodigo),
                kv("convocatoria", c.codigo()), kv("numero", numero), kv("adjudicados", adjudicados.size()));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("acta", acta.getCodigo().toString());
        detalle.put("convocatoria", c.codigo().toString());
        detalle.put("numero", numero);
        detalle.put("adjudicados", adjudicados.size());
        ActaResponse respuesta = new ActaResponse(acta.getCodigo(), numero,
                new ConvocatoriaActaResponse(c.codigo(), c.nombre()), c.gestion(), adjudicados.size(), ahora,
                nombreActor);
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), acta.getId(), detalle, true);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public VistaPreviaActa vistaPrevia(UUID convocatoriaCodigo) {
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        validar(c);

        List<Adjudicado> adjudicados = consulta.adjudicadosDelActa(c.id());
        // Un borrador no es de nadie todavia: «Generada por» queda sin dato.
        byte[] pdf = pdfService.generar(datos(c, adjudicados, "—", reloj.instant()), numeroActaService.borrador());
        return new VistaPreviaActa("acta-borrador-" + limpiar(c.nombre()) + ".pdf", pdf);
    }

    private DatosConvocatoria exigirConvocatoria(UUID codigo) {
        return consulta.convocatoria(codigo).orElseThrow(
                () -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));
    }

    /** Ya tiene acta (`ERR_ACT_02`) o sus resultados no estan publicados (`ERR_ACT_01`), en ese orden. */
    private void validar(DatosConvocatoria c) {
        if (actaRepository.existsByConvocatoriaId(c.id())) {
            throw new ConflictException(ActasErrorCodes.ERR_ACT_02, "La convocatoria ya tiene acta de adjudicación");
        }
        if (consulta.publicacion(c.id()).isEmpty()) {
            throw new ConflictException(ActasErrorCodes.ERR_ACT_01,
                    "Los resultados de la convocatoria todavía no están publicados");
        }
    }

    /** Guarda la fila; si otra peticion se adelanto pese al bloqueo, la unicidad la convierte en `ERR_ACT_02`. */
    private ActaAdjudicacion guardar(DatosConvocatoria c, String numero, String ruta, Usuario actor, Instant ahora) {
        try {
            return actaRepository.saveAndFlush(new ActaAdjudicacion(c.id(), numero, ruta, actor.getId(), ahora));
        } catch (DataIntegrityViolationException e) {
            if (violaLaUnicidadDeLaConvocatoria(e)) {
                throw new ConflictException(ActasErrorCodes.ERR_ACT_02,
                        "La convocatoria ya tiene acta de adjudicación");
            }
            throw e;
        }
    }

    /** Si algun eslabon de la cadena de causas nombra el indice unico de la convocatoria. */
    private static boolean violaLaUnicidadDeLaConvocatoria(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa.getMessage() != null && causa.getMessage().contains(INDICE_UNICO_CONVOCATORIA)) {
                return true;
            }
        }
        return false;
    }

    private static DatosActa datos(DatosConvocatoria c, List<Adjudicado> adjudicados, String generadaPor,
            Instant generadaEn) {
        return new DatosActa(c.codigo(), c.nombre(), c.gestion(), c.fechaApertura(), c.fechaCierre(), c.cupos(),
                c.puntajeMinimo(), adjudicados, generadaPor, generadaEn);
    }

    private static String nombre(Usuario actor) {
        return actor.getNombres() + " " + actor.getApellidos();
    }

    /** `actas/{codigo_convocatoria}/acta-{NNN}-{AAAA}.pdf` (la `/` del numero se reemplaza por `-`). */
    private static String ruta(UUID convocatoria, String numero) {
        return "actas/" + convocatoria + "/acta-" + numero.replace('/', '-') + ".pdf";
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
