package com.comedoruagrm.backend.features.resultados.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionLoteService;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionLoteService.AvisoParaUsuario;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.resultados.dto.ConvocatoriaResultadosResponse;
import com.comedoruagrm.backend.features.resultados.dto.PublicacionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.exception.ResultadosErrorCodes;
import com.comedoruagrm.backend.features.resultados.model.PublicacionResultado;
import com.comedoruagrm.backend.features.resultados.repository.PublicacionResultadoRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Conteos;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Destinatario;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Publicar los resultados de adjudicacion de una convocatoria (CU22). Solo el Administrador DUBSS.
 *
 * * Registra la publicacion (quien y cuando), pasa la convocatoria a «publicada», la anota en la bitacora y
 * * crea el aviso de cada finalista con decision, todo en una transaccion: si algo falla no queda nada.
 * * El push y el correo salen despues del commit, en otro hilo (CU-EXTRA-01); un fallo de envio deja el aviso
 * * en la bandeja y nunca deshace la publicacion.
 * * No hay idempotencia por clave: se bloquea la fila de la convocatoria y la segunda peticion recibe
 * * `ERR_PUB_02`; la unicidad de `convocatoria_id` es la segunda red de seguridad.
 * * No exige el acta (CU08), se publica aunque no haya adjudicados, no cambia el estado de ninguna postulacion
 * * ni crea el padron de becados, y no existe despublicar.
 * ! Ni el log ni la bitacora llevan nombres, CI, correos ni tokens.
 */
@Service
@RequiredArgsConstructor
public class PublicacionResultadosService {

    private static final Logger LOG = LoggerFactory.getLogger(PublicacionResultadosService.class);

    private final ResultadosConsultaRepository consulta;
    private final PublicacionResultadoRepository publicacionRepository;
    private final UsuarioService usuarioService;
    private final NotificacionLoteService notificacionLoteService;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.PUBLICACION_RESULTADOS, entidad = "publicacion_resultado")
    public ResultadoAuditado<PublicacionResultadoResponse> publicar(UUID actorCodigo, UUID convocatoriaCodigo) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        DatosConvocatoria buscada = exigirConvocatoria(convocatoriaCodigo);

        // Primero el bloqueo y despues se vuelve a leer todo: la peticion que llego segunda ya ve la publicacion.
        consulta.bloquearConvocatoria(buscada.id());
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        if (consulta.publicacion(c.id()).isPresent()) {
            throw new ConflictException(ResultadosErrorCodes.ERR_PUB_02,
                    "Los resultados de la convocatoria ya fueron publicados");
        }
        if (!EstadoConvocatoria.CERRADA.name().equals(c.estado())) {
            throw new ConflictException(ResultadosErrorCodes.ERR_PUB_03,
                    "La convocatoria debe estar cerrada para publicar sus resultados");
        }
        Conteos conteos = consulta.contarPorEstado(c.id());
        if (conteos.calificadas() > 0) {
            throw new ConflictException(ResultadosErrorCodes.ERR_PUB_01,
                    "Faltan " + conteos.calificadas() + " decisiones de adjudicación para publicar los resultados");
        }

        PublicacionResultado publicacion = guardar(c, actor);
        consulta.marcarConvocatoriaPublicada(c.id());
        int avisados = avisar(c, consulta.destinatariosDelAviso(c.id()));
        LOG.info("Resultados publicados", kv("module", "resultados"), kv("userId", actorCodigo),
                kv("convocatoria", c.codigo()), kv("adjudicados", conteos.adjudicados()),
                kv("noAdjudicados", conteos.noAdjudicados()), kv("avisados", avisados));

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("publicacion", publicacion.getCodigo().toString());
        detalle.put("convocatoria", c.codigo().toString());
        detalle.put("adjudicados", conteos.adjudicados());
        detalle.put("noAdjudicados", conteos.noAdjudicados());
        detalle.put("avisados", avisados);
        PublicacionResultadoResponse respuesta = new PublicacionResultadoResponse(
                new ConvocatoriaResultadosResponse(c.codigo(), c.nombre(), EstadoConvocatoria.PUBLICADA.name()),
                publicacion.getPublicadoEn(), actor.getNombres() + " " + actor.getApellidos(),
                conteos.adjudicados(), conteos.noAdjudicados(), avisados);
        return new ResultadoAuditado<>(respuesta, actor.getId(), actor.getCodigo(), publicacion.getId(), detalle,
                true);
    }

    private DatosConvocatoria exigirConvocatoria(UUID codigo) {
        return consulta.convocatoria(codigo).orElseThrow(
                () -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));
    }

    /** Guarda la fila; si otra peticion se adelanto pese al bloqueo, la unicidad la convierte en `ERR_PUB_02`. */
    private PublicacionResultado guardar(DatosConvocatoria c, Usuario actor) {
        try {
            return publicacionRepository.saveAndFlush(new PublicacionResultado(c.id(), actor.getId(), Instant.now()));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(ResultadosErrorCodes.ERR_PUB_02,
                    "Los resultados de la convocatoria ya fueron publicados");
        }
    }

    /** Un aviso por destinatario, cada uno con su resultado; se guardan por lotes y se despachan tras el commit. */
    private int avisar(DatosConvocatoria c, List<Destinatario> destinatarios) {
        List<AvisoParaUsuario> avisos = new ArrayList<>(destinatarios.size());
        for (Destinatario d : destinatarios) {
            EventoNotificacion evento = "ADJUDICADA".equals(d.resultado()) ? EventoNotificacion.BECA_ADJUDICADA
                    : EventoNotificacion.BECA_NO_ADJUDICADA;
            avisos.add(new AvisoParaUsuario(d.usuarioId(), d.correo(), evento,
                    Map.of("nombres", d.nombres(), "convocatoria", c.nombre()), d.postulacionId()));
        }
        return notificacionLoteService.notificarLote(avisos);
    }
}
