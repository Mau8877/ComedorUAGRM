package com.comedoruagrm.backend.features.panel.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.convocatorias.service.FechasConvocatoria;
import com.comedoruagrm.backend.features.panel.dto.PanelAdministradorResponse;
import com.comedoruagrm.backend.features.panel.dto.PanelAdministradorResponse.ConvocatoriaAbierta;
import com.comedoruagrm.backend.features.panel.dto.PanelComedorResponse;
import com.comedoruagrm.backend.features.panel.dto.PanelPostulanteResponse;
import com.comedoruagrm.backend.features.panel.dto.PanelComedorResponse.AsistenciaDia;
import com.comedoruagrm.backend.features.panel.repository.PanelRepository;
import com.comedoruagrm.backend.features.panel.repository.PanelRepository.AsistenciaFecha;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Resúmenes de las pantallas de inicio. Solo lectura y solo agregados (sin datos personales), por eso no se loguea
 * ni se anota en la bitácora.
 *
 * * Cada método exige el rol de su pantalla (`@PreAuthorize`): el panel de un rol no se ve con otro.
 * * "Hoy" es la fecha de Bolivia, la misma que usa el registro de asistencia (CU10).
 */
@Service
@RequiredArgsConstructor
public class PanelService {

    static final int DIAS_DE_ASISTENCIA = 7;

    private final PanelRepository repositorio;
    private final UsuarioService usuarioService;
    private final Clock reloj;

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PanelAdministradorResponse administrador() {
        ConvocatoriaAbierta abierta = repositorio.convocatoriaAbierta()
                .map(f -> new ConvocatoriaAbierta(f.codigo(), f.nombre(), f.fechaCierre(), f.cupos(),
                        f.postulaciones()))
                .orElse(null);
        return new PanelAdministradorResponse(repositorio.convocatoriasAbiertas(),
                repositorio.postulacionesEnEstado("pendiente"), repositorio.postulacionesEnEstado("calificada"),
                repositorio.becasEnEstado("VIGENTE"), repositorio.becasEnEstado("SUSPENDIDA"),
                repositorio.asistenciasDelDia(hoy()), abierta);
    }

    @PreAuthorize("hasRole('PERSONAL_COMEDOR')")
    @Transactional(readOnly = true)
    public PanelComedorResponse comedor(UUID actorCodigo) {
        LocalDate hoy = hoy();
        long actorId = usuarioService.obtenerPorCodigo(actorCodigo).getId();
        return new PanelComedorResponse(hoy, repositorio.becasEnEstado("VIGENTE"),
                repositorio.asistenciasDelDia(hoy), repositorio.asistenciasDelDiaRegistradasPor(hoy, actorId),
                repositorio.ultimoRegistroDelDia(hoy).orElse(null), ultimosDias(hoy));
    }

    @PreAuthorize("hasRole('POSTULANTE')")
    @Transactional(readOnly = true)
    public PanelPostulanteResponse postulante(UUID actorCodigo) {
        long actorId = usuarioService.obtenerPorCodigo(actorCodigo).getId();
        PanelPostulanteResponse.ConvocatoriaAbierta abierta = repositorio.convocatoriaAbierta()
                .map(f -> new PanelPostulanteResponse.ConvocatoriaAbierta(f.codigo(), f.nombre(), f.fechaCierre()))
                .orElse(null);
        String estado = abierta == null ? null : repositorio.estadoEnConvocatoriaAbierta(actorId).orElse(null);
        return new PanelPostulanteResponse(abierta, "borrador".equals(estado),
                estado != null && !"borrador".equals(estado));
    }

    /** Los últimos 7 días terminando hoy, con 0 en los que no tuvieron registros. */
    private List<AsistenciaDia> ultimosDias(LocalDate hoy) {
        LocalDate desde = hoy.minusDays(DIAS_DE_ASISTENCIA - 1L);
        Map<LocalDate, Long> porFecha = new HashMap<>();
        for (AsistenciaFecha fila : repositorio.asistenciasPorFecha(desde, hoy)) {
            porFecha.put(fila.fecha(), fila.asistencias());
        }
        List<AsistenciaDia> dias = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hoy); dia = dia.plusDays(1)) {
            dias.add(new AsistenciaDia(dia, porFecha.getOrDefault(dia, 0L)));
        }
        return dias;
    }

    private LocalDate hoy() {
        return FechasConvocatoria.aFecha(reloj.instant());
    }
}
