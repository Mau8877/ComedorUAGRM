package com.comedoruagrm.backend.features.dashboard.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.dashboard.dto.FiltrosDashboardResponse;
import com.comedoruagrm.backend.features.dashboard.dto.FiltrosDashboardResponse.ConvocatoriaFiltro;
import com.comedoruagrm.backend.features.dashboard.dto.FiltrosDashboardResponse.GestionFiltro;
import com.comedoruagrm.backend.features.dashboard.repository.FiltrosDashboardRepository;
import com.comedoruagrm.backend.features.dashboard.repository.FiltrosDashboardRepository.ConvocatoriaFila;
import com.comedoruagrm.backend.features.dashboard.repository.FiltrosDashboardRepository.GestionFila;

import lombok.RequiredArgsConstructor;

/**
 * Catalogo de filtros del dashboard del Directivo (CU12) y la gestion que se muestra por defecto.
 *
 * * La gestion vigente es la que contiene la fecha de hoy en La Paz; si ninguna la contiene, la de inicio mas
 * * reciente; sin gestiones no hay ninguna (no es un error).
 * * Solo lee y no se loguea (ver LOGGING_BACKEND.md); tampoco se anota en la bitacora.
 */
@Service
@RequiredArgsConstructor
public class FiltrosDashboardService {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

    private final FiltrosDashboardRepository repositorio;
    private final Clock reloj;

    /** La gestion que se muestra por defecto. */
    public record GestionVigente(long id, UUID codigo, String etiqueta) {
    }

    @PreAuthorize("hasRole('DIRECTIVO')")
    @Transactional(readOnly = true)
    public FiltrosDashboardResponse obtener() {
        List<GestionFila> gestiones = repositorio.gestiones();
        Map<Long, List<ConvocatoriaFiltro>> porGestion = new HashMap<>();
        for (ConvocatoriaFila c : repositorio.convocatorias()) {
            porGestion.computeIfAbsent(c.gestionId(), k -> new ArrayList<>())
                    .add(new ConvocatoriaFiltro(c.codigo(), c.nombre()));
        }
        Long vigente = elegirVigente(gestiones).map(GestionFila::id).orElse(null);
        List<GestionFiltro> respuesta = gestiones.stream()
                .map(g -> new GestionFiltro(g.codigo(), g.etiqueta(), vigente != null && vigente == g.id(),
                        porGestion.getOrDefault(g.id(), List.of())))
                .toList();
        return new FiltrosDashboardResponse(respuesta);
    }

    @PreAuthorize("hasRole('DIRECTIVO')")
    @Transactional(readOnly = true)
    public Optional<GestionVigente> gestionVigente() {
        return elegirVigente(repositorio.gestiones()).map(g -> new GestionVigente(g.id(), g.codigo(), g.etiqueta()));
    }

    /** `gestiones` llega de la mas reciente a la mas antigua: si ninguna contiene hoy, gana la primera. */
    private Optional<GestionFila> elegirVigente(List<GestionFila> gestiones) {
        LocalDate hoy = LocalDate.now(reloj.withZone(LA_PAZ));
        return gestiones.stream().filter(g -> !hoy.isBefore(g.fechaInicio()) && !hoy.isAfter(g.fechaFin()))
                .findFirst().or(() -> gestiones.stream().findFirst());
    }
}
