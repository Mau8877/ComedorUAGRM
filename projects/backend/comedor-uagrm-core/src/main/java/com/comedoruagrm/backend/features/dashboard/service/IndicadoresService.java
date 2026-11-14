package com.comedoruagrm.backend.features.dashboard.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
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
 * ? La asistencia no existe todavia: vale null en los totales y en cada mes.
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

    /** La consulta sin la comprobacion de rol: la reutiliza el PDF, que ya la hizo en su propio metodo. */
    IndicadoresResponse obtener(UUID gestionCodigo, UUID convocatoriaCodigo) {
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
        if (gestion == null) {
            return new IndicadoresResponse(null, null, new Totales(0, 0, 0, null), List.of(), null, true);
        }
        Long convocatoriaId = convocatoria == null ? null : convocatoria.id();
        TotalesFila totales = repositorio.totales(gestion.id(), convocatoriaId);
        // TODO CU-10: calcular el promedio de asistencia cuando exista el registro de asistencia
        Totales respuestaTotales = new Totales(totales.postulaciones(), totales.finalistas(), totales.adjudicadas(),
                null);
        boolean sinDatos = totales.postulaciones() == 0 && totales.finalistas() == 0 && totales.adjudicadas() == 0;
        return new IndicadoresResponse(new GestionResumen(gestion.codigo(), gestion.etiqueta()),
                convocatoria == null ? null : new ConvocatoriaResumen(convocatoria.codigo(), convocatoria.nombre()),
                respuestaTotales, serieSinHuecos(repositorio.serieMensual(gestion.id(), convocatoriaId)),
                repositorio.ultimaActualizacion().orElse(null), sinDatos);
    }

    private static List<IndicadorMensual> serieSinHuecos(List<FilaMensual> filas) {
        if (filas.isEmpty()) {
            return List.of();
        }
        Map<YearMonth, FilaMensual> porMes = new HashMap<>();
        for (FilaMensual f : filas) {
            porMes.put(YearMonth.from(f.mes()), f);
        }
        YearMonth primero = YearMonth.from(filas.get(0).mes());
        YearMonth ultimo = YearMonth.from(filas.get(filas.size() - 1).mes());
        List<IndicadorMensual> serie = new ArrayList<>();
        for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            FilaMensual f = porMes.getOrDefault(mes, new FilaMensual(LocalDate.of(mes.getYear(), mes.getMonth(), 1),
                    0, 0, 0));
            serie.add(new IndicadorMensual(mes.toString(), f.postulaciones(), f.finalistas(), f.adjudicadas(), null));
        }
        return serie;
    }

    private static NotFoundException noExiste() {
        return new NotFoundException(DashboardErrorCodes.ERR_DAS_01,
                "La gestión o la convocatoria no existe, o la convocatoria no es de esa gestión.");
    }
}
