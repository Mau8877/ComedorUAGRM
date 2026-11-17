package com.comedoruagrm.backend.features.postulaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.BorradorGuardadoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.BorradorResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios;
import com.comedoruagrm.backend.features.postulaciones.dto.GuardarBorradorRequest;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.EnvioPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.BorradorAbierto;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.CabeceraEnvio;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.Respuesta;

import lombok.RequiredArgsConstructor;

/**
 * Guardar y retomar el borrador de la postulacion (CU17, extiende al CU02). Solo el Postulante dueño.
 *
 * * {@link #guardar}: guarda el avance del formulario SIN exigir los campos ni los documentos obligatorios, con la
 * * fila bloqueada. La convocatoria debe seguir abierta (se valida con el CU18, igual que el envio: A1). Un valor
 * * presente pero invalido (rango, formato u opcion) rechaza todo el guardado con `ERR_ENV_05`.
 * * No cambia el estado de la postulacion, no la manda a la IA y no cuenta como postulacion vigente.
 * * {@link #obtener}: «Mi borrador»; retomarlo para editar es «Nueva postulacion» del CU02, que ya devuelve las
 * * respuestas guardadas.
 * * Al cerrar la convocatoria los borradores quedan bloqueados (no hay job que los borre): no se pueden guardar ni
 * * enviar y dejan de listarse.
 * ! No se loguean las respuestas del formulario ni datos personales, y no se anota en la bitacora.
 */
@Service
@RequiredArgsConstructor
public class BorradorPostulacionService {

    private static final Logger LOG = LoggerFactory.getLogger(BorradorPostulacionService.class);

    private final EnvioPostulacionConsultaRepository consulta;
    private final DocumentosPostulacionConsultaRepository documentos;
    private final ValidacionPostulacionService validacion;
    private final Clock reloj;

    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional
    public BorradorGuardadoResponse guardar(UUID usuarioCodigo, UUID postulacionCodigo,
            GuardarBorradorRequest request) {
        CabeceraEnvio cabecera = consulta.cabecera(postulacionCodigo, usuarioCodigo).orElseThrow(
                () -> new NotFoundException(EnvioPostulacionErrorCodes.ERR_ENV_02, "La postulación no existe"));
        consulta.bloquearPostulacion(cabecera.postulacionId());
        // Se relee el estado despues del bloqueo: un envio que llego antes ya la dejo en `pendiente`.
        EstadoPostulacion estado = EstadoPostulacion.valueOf(
                consulta.estadoActual(cabecera.postulacionId()).orElseThrow());
        if (estado != EstadoPostulacion.borrador && estado != EstadoPostulacion.devuelta) {
            throw new ConflictException(EnvioPostulacionErrorCodes.ERR_ENV_03,
                    "Esta postulación ya no se puede editar");
        }

        validacion.validar(usuarioCodigo, cabecera.convocatoriaCodigo());
        Respuesta respuesta = limpiar(request);
        DatosComplementarios complementarios = ReglasRespuestaSocioeconomica.limpiar(
                request == null ? null : request.complementarios());
        List<String> invalidos = new ArrayList<>(ReglasRespuestaSocioeconomica.invalidos(respuesta));
        invalidos.addAll(ReglasRespuestaSocioeconomica.invalidosComplementarios(complementarios));
        if (!invalidos.isEmpty()) {
            LOG.warn("Borrador rechazado: datos invalidos", kv("module", "postulaciones"),
                    kv("errorCode", EnvioPostulacionErrorCodes.ERR_ENV_05));
            throw new ValidationException(EnvioPostulacionErrorCodes.ERR_ENV_05,
                    "Revisa el formulario: " + String.join("; ", invalidos));
        }

        consulta.guardarRespuesta(cabecera.postulacionId(), respuesta);
        consulta.guardarComplementarios(cabecera.postulacionId(), ComplementariosJson.escribir(complementarios));
        LOG.info("Borrador guardado", kv("module", "postulaciones"), kv("userId", usuarioCodigo));
        return new BorradorGuardadoResponse(cabecera.postulacionCodigo(),
                consulta.guardadoEn(cabecera.postulacionId()).orElse(reloj.instant()),
                pendientes(respuesta, complementarios));
    }

    /** El borrador del usuario en la convocatoria abierta; vacio si no tiene o si la convocatoria ya cerro. */
    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional(readOnly = true)
    public Optional<BorradorResumenResponse> obtener(UUID usuarioCodigo) {
        return consulta.borradorAbierto(usuarioCodigo, reloj.instant()).map(this::resumen);
    }

    private BorradorResumenResponse resumen(BorradorAbierto b) {
        Respuesta respuesta = consulta.respuesta(b.postulacionId())
                .orElse(new Respuesta(null, null, null, null, null, null));
        Set<Long> conArchivo = new HashSet<>();
        documentos.adjuntos(b.postulacionId()).forEach(a -> conArchivo.add(a.tipoDocumentoId()));
        int documentosPendientes = (int) documentos.requeridos(b.convocatoriaId()).stream()
                .filter(d -> d.obligatorio() && !conArchivo.contains(d.tipoDocumentoId())).count();
        DatosComplementarios complementarios = ComplementariosJson.leer(
                consulta.complementarios(b.postulacionId()).orElse(null));
        return new BorradorResumenResponse(b.codigo(), b.convocatoria(), b.gestion(), b.fechaCierre(),
                consulta.guardadoEn(b.postulacionId()).orElse(null), pendientes(respuesta, complementarios),
                documentosPendientes);
    }

    /** Cuantos campos obligatorios del formulario siguen vacios: los seis de columna y los complementarios. */
    private static int pendientes(Respuesta r, DatosComplementarios complementarios) {
        int vacios = ReglasRespuestaSocioeconomica.faltantesComplementarios(complementarios).size();
        vacios += r.ingresosFamiliares() == null ? 1 : 0;
        vacios += r.dependientes() == null ? 1 : 0;
        vacios += r.procedencia() == null ? 1 : 0;
        vacios += r.patrimonio() == null ? 1 : 0;
        vacios += r.estadoVulnerabilidad() == null ? 1 : 0;
        vacios += r.promedioAcademico() == null ? 1 : 0;
        return vacios;
    }

    /** Los textos en blanco se guardan como vacios (nulos). */
    private static Respuesta limpiar(GuardarBorradorRequest r) {
        if (r == null) {
            return new Respuesta(null, null, null, null, null, null);
        }
        return new Respuesta(r.ingresosFamiliares(), r.dependientes(),
                ReglasRespuestaSocioeconomica.limpio(r.procedencia()),
                ReglasRespuestaSocioeconomica.limpio(r.patrimonio()),
                ReglasRespuestaSocioeconomica.limpio(r.estadoVulnerabilidad()), r.promedioAcademico());
    }
}
