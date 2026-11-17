package com.comedoruagrm.backend.features.postulaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.ValidacionPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.ValidacionPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.ValidacionPostulacionConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.repository.ValidacionPostulacionConsultaRepository.DatosPostulante;
import com.comedoruagrm.backend.features.postulaciones.repository.ValidacionPostulacionConsultaRepository.PostulacionExistente;

import lombok.RequiredArgsConstructor;

/**
 * Validacion de elegibilidad y duplicados (CU18). Servicio interno que el CU02 llama al enviar una postulacion.
 *
 * * Solo lectura: no crea ni modifica nada, asi que repetirla da el mismo resultado (RNF-03, A4). No tiene
 * * endpoint propio: el CU02 la usa dentro de su misma transaccion.
 * * «Vigente» = postulacion ya enviada (no `borrador`) del postulante en cualquier convocatoria de la
 * * misma gestion. Un `borrador` es la propia postulacion que se envia; una `devuelta` de esta misma
 * * convocatoria es el reenvio (CU19, A3).
 * * Las condiciones de elegibilidad que defina la DUBSS se agregan en {@link #verificarCondiciones}.
 * ! Una violacion del indice unico `uq_postulacion_postulante_convocatoria` (dos envios simultaneos) la
 * ! traduce el CU02 a `ERR_VAL_01`: esta validacion no bloquea filas.
 */
@Service
@RequiredArgsConstructor
public class ValidacionPostulacionService {

    private static final Logger LOG = LoggerFactory.getLogger(ValidacionPostulacionService.class);
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ValidacionPostulacionConsultaRepository consulta;
    private final Clock reloj;

    /**
     * Devuelve «valida» o lanza el motivo del rechazo. `usuarioCodigo` es el del token verificado.
     *
     * @throws NotFoundException  `ERR_CV_01`: la convocatoria no existe
     * @throws ForbiddenException `ERR_VAL_02`: no es elegible
     * @throws ConflictException  `ERR_VAL_03` (convocatoria cerrada o fuera de fechas) o `ERR_VAL_01` (duplicada)
     */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional(readOnly = true)
    public ResultadoValidacion validar(UUID usuarioCodigo, UUID convocatoriaCodigo) {
        DatosConvocatoria convocatoria = consulta.convocatoria(convocatoriaCodigo).orElseThrow(
                () -> new NotFoundException(ConvocatoriaErrorCodes.ERR_CV_01, "La convocatoria no existe"));
        DatosPostulante postulante = consulta.postulanteDe(usuarioCodigo).orElseThrow(() -> rechazoNoElegible(
                "Solo un postulante puede enviar una postulación"));

        verificarCondiciones(postulante);
        verificarConvocatoriaAbierta(convocatoria);
        UUID reenvio = verificarSinDuplicados(postulante, convocatoria);
        return new ResultadoValidacion(postulante.id(), convocatoria.id(), reenvio);
    }

    /**
     * Condiciones de elegibilidad. Por definir con la DUBSS: hoy solo la cuenta activa; las nuevas
     * condiciones se suman aca, cada una con su propio rechazo `ERR_VAL_02`.
     */
    private void verificarCondiciones(DatosPostulante postulante) {
        if (!postulante.activo()) {
            throw rechazoNoElegible("Tu cuenta está desactivada");
        }
    }

    private void verificarConvocatoriaAbierta(DatosConvocatoria c) {
        Instant ahora = reloj.instant();
        if (!"ABIERTA".equals(c.estado()) || ahora.isBefore(c.fechaApertura()) || ahora.isAfter(c.fechaCierre())) {
            LOG.warn("Postulacion rechazada: la convocatoria no recibe postulaciones", kv("module", "postulaciones"),
                    kv("errorCode", ValidacionPostulacionErrorCodes.ERR_VAL_03));
            throw new ConflictException(ValidacionPostulacionErrorCodes.ERR_VAL_03,
                    "La convocatoria no está recibiendo postulaciones");
        }
    }

    /** El codigo de la postulacion `devuelta` a reenviar, o `null` si es un envio nuevo. */
    private UUID verificarSinDuplicados(DatosPostulante postulante, DatosConvocatoria convocatoria) {
        UUID reenvio = null;
        for (PostulacionExistente p : consulta.postulacionesEnLaGestion(postulante.id(), convocatoria.gestionId())) {
            EstadoPostulacion estado = EstadoPostulacion.valueOf(p.estado());
            if (estado == EstadoPostulacion.borrador) {
                continue; // la propia postulacion que se envia (CU17), o un borrador que no se envio nunca
            }
            if (estado == EstadoPostulacion.devuelta && p.convocatoriaId() == convocatoria.id()) {
                reenvio = p.codigo();
                continue;
            }
            LOG.warn("Postulacion rechazada: ya hay una vigente en la gestion", kv("module", "postulaciones"),
                    kv("errorCode", ValidacionPostulacionErrorCodes.ERR_VAL_01));
            throw new ConflictException(ValidacionPostulacionErrorCodes.ERR_VAL_01, mensajeDuplicada(
                    convocatoria.gestionEtiqueta(), p));
        }
        return reenvio;
    }

    private static String mensajeDuplicada(String gestion, PostulacionExistente p) {
        // La decision final del CU05 no se revela hasta publicar los resultados: se ve «calificada».
        EstadoPostulacion visible = EstadoVisible.calcular(EstadoPostulacion.valueOf(p.estado()), false);
        String enviada = p.fechaEnvio() == null ? "" : " enviada el " + FECHA.format(p.fechaEnvio().atZone(ZONA));
        return "Ya tienes una postulación vigente en la gestión " + gestion + " (" + visible.name()
                + enviada + "). Solo se permite una por gestión";
    }

    private ForbiddenException rechazoNoElegible(String motivo) {
        LOG.warn("Postulacion rechazada: el postulante no es elegible", kv("module", "postulaciones"),
                kv("errorCode", ValidacionPostulacionErrorCodes.ERR_VAL_02));
        return new ForbiddenException(ValidacionPostulacionErrorCodes.ERR_VAL_02, motivo);
    }
}
