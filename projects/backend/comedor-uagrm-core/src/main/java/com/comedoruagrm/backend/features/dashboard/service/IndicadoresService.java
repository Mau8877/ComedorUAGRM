package com.comedoruagrm.backend.features.dashboard.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.ConvocatoriaResumen;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.GestionResumen;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.IndicadorMensual;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.Totales;
import com.comedoruagrm.backend.features.dashboard.exception.DashboardErrorCodes;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.ConvocatoriaDatos;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.FilaAsistenciaMes;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.FilaAsistenciaTotal;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.FilaMensual;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.GestionDatos;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.TotalesFila;

import lombok.RequiredArgsConstructor;

/**
 * Indicadores del dashboard del Directivo (CU12). Solo lectura: no se loguea ni se anota en la bitacora (son
 * agregados sin datos personales).
 *
 * * Sin filtros se muestra la gestion vigente; con solo la convocatoria, la gestion de esa convocatoria; con
 * * ambas se comprueba que coincidan. Cualquier incoherencia es `ERR_DAS_01`.
 * * Sin gestiones la respuesta viene vacia (no es un error).
 * * `porMes` se completa con ceros entre el primer y el ultimo mes con datos, para que el grafico sea continuo.
 * * La asistencia sale de `mv_bi_asistencia_dia` (CU10): asistieron / esperados, en porcentaje. Vale null en los
 * * totales o en un mes cuando no hubo asistencia que medir (se muestra «Sin datos»).
 */
@Service
@RequiredArgsConstructor
public class IndicadoresService {

    private final IndicadoresRepository repositorio;
    private final FiltrosDashboardService filtros;

    @PreAuthorize("hasRole('DIRECTIVO')")
    @Transactional(readOnly = true)
    public IndicadoresResponse consultar(UUID gestion, UUID convocatoria) {
        return obtener(gestion, convocatoria);
    }

    /** La gestion y, si se pidio, la convocatoria ya resueltas. `gestion` es null si todavia no hay gestiones. */
    record Seleccion(GestionDatos gestion, ConvocatoriaDatos convocatoria) {
    }

    /**
     * Resuelve los filtros: sin filtros, la gestion vigente; con solo la convocatoria, la gestion de esa
     * convocatoria; con ambas se comprueba que coincidan (`ERR_DAS_01`). Lo reutiliza el analisis BI.
     */
    Seleccion resolver(UUID gestionCodigo, UUID convocatoriaCodigo) {
        ConvocatoriaDatos convocatoria = null;
        GestionDatos gestion;
        if (convocatoriaCodigo != null) {
            convocatoria = repositorio.convocatoria(convocatoriaCodigo).orElseThrow(IndicadoresService::noExiste);
            gestion = convocatoria.gestion();
            if (gestionCodigo != null && !gestionCodigo.equals(gestion.codigo())) {
                throw noExiste();
            }
        } else if (gestionCodigo != null) {
            gestion = repositorio.gestion(gestionCodigo).orElseThrow(IndicadoresService::noExiste);
        } else {
            gestion = filtros.gestionVigente().map(g -> new GestionDatos(g.id(), g.codigo(), g.etiqueta()))
                    .orElse(null);
        }
        return new Seleccion(gestion, convocatoria);
    }

    /** La consulta sin la comprobacion de rol: la reutiliza el PDF, que ya la hizo en su propio metodo. */
    IndicadoresResponse obtener(UUID gestionCodigo, UUID convocatoriaCodigo) {
        Seleccion seleccion = resolver(gestionCodigo, convocatoriaCodigo);
        GestionDatos gestion = seleccion.gestion();
        ConvocatoriaDatos convocatoria = seleccion.convocatoria();
        if (gestion == null) {
            return new IndicadoresResponse(null, null, new Totales(0, 0, 0, null), List.of(), null, true);
        }
        Long convocatoriaId = convocatoria == null ? null : convocatoria.id();
        TotalesFila totales = repositorio.totales(gestion.id(), convocatoriaId);
        FilaAsistenciaTotal asistencia = repositorio.asistenciaTotal(gestion.id(), convocatoriaId);
        Totales respuestaTotales = new Totales(totales.postulaciones(), totales.finalistas(), totales.adjudicadas(),
                porcentaje(asistencia.asistieron(), asistencia.esperados()));
        boolean sinDatos = totales.postulaciones() == 0 && totales.finalistas() == 0 && totales.adjudicadas() == 0;
        return new IndicadoresResponse(new GestionResumen(gestion.codigo(), gestion.etiqueta()),
                convocatoria == null ? null : new ConvocatoriaResumen(convocatoria.codigo(), convocatoria.nombre()),
                respuestaTotales,
                serieSinHuecos(repositorio.serieMensual(gestion.id(), convocatoriaId),
                        repositorio.asistenciaMensual(gestion.id(), convocatoriaId)),
                repositorio.ultimaActualizacion().orElse(null), sinDatos);
    }

    /** Asistencia en porcentaje con un decimal; null si no hubo asistencia esperada (nada que medir). */
    static Double porcentaje(long parte, long total) {
        return total <= 0 ? null : Math.round(parte * 1000.0 / total) / 10.0;
    }

    private static List<IndicadorMensual> serieSinHuecos(List<FilaMensual> filas, List<FilaAsistenciaMes> asistencia) {
        if (filas.isEmpty() && asistencia.isEmpty()) {
            return List.of();
        }
        Map<YearMonth, FilaMensual> porMes = new HashMap<>();
        for (FilaMensual f : filas) {
            porMes.put(YearMonth.from(f.mes()), f);
        }
        Map<YearMonth, FilaAsistenciaMes> asistenciaPorMes = new HashMap<>();
        for (FilaAsistenciaMes f : asistencia) {
            asistenciaPorMes.put(YearMonth.from(f.mes()), f);
        }
        // El rango cubre tambien los meses que solo tienen asistencia (la asistencia llega despues de la postulacion).
        YearMonth primero = porMes.keySet().stream().min(YearMonth::compareTo)
                .orElseGet(() -> Collections.min(asistenciaPorMes.keySet()));
        YearMonth ultimo = porMes.keySet().stream().max(YearMonth::compareTo)
                .orElseGet(() -> Collections.max(asistenciaPorMes.keySet()));
        if (!asistenciaPorMes.isEmpty()) {
            primero = min(primero, Collections.min(asistenciaPorMes.keySet()));
            ultimo = max(ultimo, Collections.max(asistenciaPorMes.keySet()));
        }
        List<IndicadorMensual> serie = new ArrayList<>();
        for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            FilaMensual f = porMes.getOrDefault(mes, new FilaMensual(LocalDate.of(mes.getYear(), mes.getMonth(), 1),
                    0, 0, 0));
            FilaAsistenciaMes a = asistenciaPorMes.get(mes);
            serie.add(new IndicadorMensual(mes.toString(), f.postulaciones(), f.finalistas(), f.adjudicadas(),
                    a == null ? null : porcentaje(a.asistieron(), a.esperados())));
        }
        return serie;
    }

    private static YearMonth min(YearMonth a, YearMonth b) {
        return a.isBefore(b) ? a : b;
    }

    private static YearMonth max(YearMonth a, YearMonth b) {
        return a.isAfter(b) ? a : b;
    }

    private static NotFoundException noExiste() {
        return new NotFoundException(DashboardErrorCodes.ERR_DAS_01,
                "La gestión o la convocatoria no existe, o la convocatoria no es de esa gestión.");
    }
}
