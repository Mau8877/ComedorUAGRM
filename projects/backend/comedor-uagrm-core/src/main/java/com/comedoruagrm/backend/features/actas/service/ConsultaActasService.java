package com.comedoruagrm.backend.features.actas.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.actas.dto.AccesoActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaPendienteActaResponse;
import com.comedoruagrm.backend.features.actas.exception.ActasErrorCodes;
import com.comedoruagrm.backend.features.actas.model.ActaAdjudicacion;
import com.comedoruagrm.backend.features.actas.repository.ActaAdjudicacionRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Listar, consultar y descargar las actas de adjudicacion (CU08). Solo el Administrador DUBSS.
 *
 * * Listar, ver las pendientes y consultar un acta no se anotan en la bitacora. Descargar si (el PDF lleva CI):
 * * entrega una direccion temporal y firmada de MinIO (`AlmacenamientoService.VIGENCIA_ACCESO`), nunca el
 * * binario ni la ruta del archivo.
 * * Si el acta esta registrada pero su archivo no esta en el almacenamiento responde `ERR_ACT_04` y no anota la
 * * descarga (la anotacion solo se hace si el metodo termina bien).
 * ! Ni las respuestas, ni la bitacora ni los logs llevan la ruta del archivo ni la direccion firmada.
 */
@Service
@RequiredArgsConstructor
public class ConsultaActasService {

    private final ActasConsultaRepository consulta;
    private final ActaAdjudicacionRepository actaRepository;
    private final AlmacenamientoService almacenamiento;
    private final UsuarioService usuarioService;
    private final Clock reloj;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public Page<ActaResponse> listar(String search, Pageable pageable) {
        return consulta.actas(search, pageable);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public Page<ConvocatoriaPendienteActaResponse> pendientes(Pageable pageable) {
        return consulta.convocatoriasPendientes(pageable);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public ActaResponse obtener(UUID convocatoriaCodigo) {
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        return consulta.acta(c.id()).orElseThrow(this::sinActa);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.DESCARGA_ACTA, entidad = "acta_adjudicacion")
    public ResultadoAuditado<AccesoActaResponse> descargar(UUID actorCodigo, UUID convocatoriaCodigo) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        DatosConvocatoria c = exigirConvocatoria(convocatoriaCodigo);
        ActaAdjudicacion acta = actaRepository.findByConvocatoriaId(c.id()).orElseThrow(this::sinActa);
        if (!almacenamiento.existe(acta.getStoragePath())) {
            throw new NotFoundException(ActasErrorCodes.ERR_ACT_04,
                    "El archivo del acta no está disponible. Avisa a soporte");
        }

        Instant expiraEn = reloj.instant().plus(AlmacenamientoService.VIGENCIA_ACCESO);
        String url = almacenamiento.urlTemporal(acta.getStoragePath(), AlmacenamientoService.VIGENCIA_ACCESO);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("acta", acta.getCodigo().toString());
        detalle.put("convocatoria", c.codigo().toString());
        return new ResultadoAuditado<>(new AccesoActaResponse(url, expiraEn), actor.getId(), actor.getCodigo(),
                acta.getId(), detalle, true);
    }

    private DatosConvocatoria exigirConvocatoria(UUID codigo) {
        return consulta.convocatoria(codigo).orElseThrow(
                () -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));
    }

    private NotFoundException sinActa() {
        return new NotFoundException(ActasErrorCodes.ERR_ACT_03, "La convocatoria todavía no tiene acta");
    }
}
