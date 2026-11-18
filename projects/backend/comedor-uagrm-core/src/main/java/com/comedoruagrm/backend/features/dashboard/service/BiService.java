package com.comedoruagrm.backend.features.dashboard.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.dashboard.dto.BiResponse;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.Asistencia;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.AsistenciaDia;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.Carrera;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.Cobertura;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.ConvocatoriaComparada;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.Embudo;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.Facultad;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.GestionTendencia;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.TramoPuntaje;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.ConvocatoriaResumen;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.GestionResumen;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaAsistenciaBi;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaCarrera;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaConvocatoria;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaDiaSemana;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaGestion;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaTramo;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresService.Seleccion;

import lombok.RequiredArgsConstructor;

/**
 * Analisis BI del dashboard del Directivo (CU12): embudo del proceso, comparacion entre convocatorias y entre
 * facultades, perfil de puntajes, cobertura de cupos, asistencia y tendencia entre gestiones.
 *
 * * Solo lectura y sin datos personales: igual que {@link IndicadoresService} no se loguea ni se anota en la
 * * bitacora. Resuelve los filtros con el mismo criterio (`IndicadoresService.resolver`).
 * * Lo comparativo (`convocatorias`, `tendencia`) no se achica con el filtro de convocatoria: la seleccionada solo
 * * se marca. Lo demas (embudo, facultades, puntaje, cobertura, asistencia) si respeta el filtro.
 * * Un porcentaje es null cuando su denominador es cero.
 */
@Service
@RequiredArgsConstructor
public class BiService {

    private static final int TRAMOS = 7;

    private final BiRepository repositorio;
    private final IndicadoresRepository indicadoresRepositorio;
    private final IndicadoresService indicadores;

    @PreAuthorize("hasRole('DIRECTIVO')")
    @Transactional(readOnly = true)
    public BiResponse consultar(UUID gestion, UUID convocatoria) {
        return obtener(gestion, convocatoria);
    }

    /** La consulta sin la comprobacion de rol: la reutiliza el Excel, que ya la hizo en su propio metodo. */
    BiResponse obtener(UUID gestionCodigo, UUID convocatoriaCodigo) {
        Seleccion seleccion = indicadores.resolver(gestionCodigo, convocatoriaCodigo);
        if (seleccion.gestion() == null) {
            return vacia();
        }
        long gestionId = seleccion.gestion().id();
        Long convocatoriaId = seleccion.convocatoria() == null ? null : seleccion.convocatoria().id();

        List<FilaConvocatoria> todas = repositorio.convocatorias(gestionId);
        List<FilaConvocatoria> elegidas = convocatoriaId == null ? todas
                : todas.stream().filter(f -> f.id() == convocatoriaId).toList();

        Embudo embudo = embudo(elegidas);
        boolean sinDatos = embudo.postulaciones() == 0 && embudo.finalistas() == 0 && embudo.adjudicadas() == 0;
        return new BiResponse(new GestionResumen(seleccion.gestion().codigo(), seleccion.gestion().etiqueta()),
                seleccion.convocatoria() == null ? null
                        : new ConvocatoriaResumen(seleccion.convocatoria().codigo(),
                                seleccion.convocatoria().nombre()),
                embudo, facultades(repositorio.carreras(gestionId, convocatoriaId)),
                comparativo(todas, convocatoriaId), tramos(repositorio.tramosPuntaje(gestionId, convocatoriaId)),
                puntajePromedio(elegidas), cobertura(elegidas),
                asistencia(repositorio.asistenciaTotal(gestionId, convocatoriaId),
                        repositorio.asistenciaPorDiaSemana(gestionId, convocatoriaId), elegidas),
                tendencia(repositorio.gestiones(), seleccion.gestion().codigo()),
                indicadoresRepositorio.ultimaActualizacion().orElse(null), sinDatos);
    }

    private static BiResponse vacia() {
        return new BiResponse(null, null, new Embudo(0, 0, 0, 0, 0), List.of(), List.of(), tramos(List.of()), null,
                new Cobertura(0, 0, 0, 0, 0, null), new Asistencia(null, 0, 0, dias(List.of())), List.of(), null,
                true);
    }

    private static Embudo embudo(List<FilaConvocatoria> filas) {
        return new Embudo(suma(filas, FilaConvocatoria::recibidas), suma(filas, FilaConvocatoria::precalificadas),
                suma(filas, FilaConvocatoria::finalistas), suma(filas, FilaConvocatoria::adjudicadas),
                suma(filas, FilaConvocatoria::becasVigentes));
    }

    private static List<Facultad> facultades(List<FilaCarrera> filas) {
        Map<String, List<FilaCarrera>> porFacultad = new LinkedHashMap<>();
        for (FilaCarrera f : filas) {
            porFacultad.computeIfAbsent(f.facultad(), k -> new ArrayList<>()).add(f);
        }
        long total = filas.stream().mapToLong(FilaCarrera::postulaciones).sum();
        List<Facultad> facultades = new ArrayList<>();
        for (Map.Entry<String, List<FilaCarrera>> e : porFacultad.entrySet()) {
            long postulaciones = e.getValue().stream().mapToLong(FilaCarrera::postulaciones).sum();
            long adjudicadas = e.getValue().stream().mapToLong(FilaCarrera::adjudicadas).sum();
            List<Carrera> carreras = e.getValue().stream()
                    .map(c -> new Carrera(c.carrera(), c.postulaciones(), c.adjudicadas())).toList();
            facultades.add(new Facultad(e.getKey(), postulaciones, adjudicadas, porcentaje(postulaciones, total),
                    porcentaje(adjudicadas, postulaciones), carreras));
        }
        // La que mas postula primero; a igualdad, por nombre para que el orden sea estable.
        facultades.sort((a, b) -> a.postulaciones() != b.postulaciones()
                ? Long.compare(b.postulaciones(), a.postulaciones()) : a.nombre().compareTo(b.nombre()));
        return facultades;
    }

    private static List<ConvocatoriaComparada> comparativo(List<FilaConvocatoria> filas, Long seleccionadaId) {
        List<ConvocatoriaComparada> resultado = new ArrayList<>();
        FilaConvocatoria anterior = null;
        for (FilaConvocatoria f : filas) {
            resultado.add(new ConvocatoriaComparada(f.codigo(), f.nombre(), f.cupos(), f.recibidas(), f.finalistas(),
                    f.adjudicadas(), porcentaje(f.adjudicadas(), f.recibidas()), f.puntajePromedio(),
                    anterior == null ? null : variacion(f.recibidas(), anterior.recibidas()),
                    seleccionadaId != null && seleccionadaId == f.id()));
            anterior = f;
        }
        return resultado;
    }

    /** Siempre 7 tramos (0-10 ... 60-70), con cero donde no hay nadie: el histograma no tiene huecos. */
    private static List<TramoPuntaje> tramos(List<FilaTramo> filas) {
        long[] cantidades = new long[TRAMOS];
        for (FilaTramo f : filas) {
            if (f.tramo() >= 0 && f.tramo() < TRAMOS) {
                cantidades[f.tramo()] += f.cantidad();
            }
        }
        List<TramoPuntaje> tramos = new ArrayList<>();
        for (int i = 0; i < TRAMOS; i++) {
            tramos.add(new TramoPuntaje(i * 10, i * 10 + 10, cantidades[i]));
        }
        return tramos;
    }

    /** Promedio ponderado por las postulaciones precalificadas de cada convocatoria; null si no hay puntajes. */
    private static Double puntajePromedio(List<FilaConvocatoria> filas) {
        double suma = 0;
        long cantidad = 0;
        for (FilaConvocatoria f : filas) {
            if (f.puntajePromedio() != null && f.precalificadas() > 0) {
                suma += f.puntajePromedio() * f.precalificadas();
                cantidad += f.precalificadas();
            }
        }
        return cantidad == 0 ? null : Math.round(suma / cantidad * 10) / 10.0;
    }

    private static Cobertura cobertura(List<FilaConvocatoria> filas) {
        long cupos = filas.stream().mapToLong(f -> f.cupos() == null ? 0 : f.cupos()).sum();
        long vigentes = suma(filas, FilaConvocatoria::becasVigentes);
        return new Cobertura(cupos, vigentes, suma(filas, FilaConvocatoria::becasSuspendidas),
                suma(filas, FilaConvocatoria::becasBaja), suma(filas, FilaConvocatoria::reemplazos),
                porcentaje(vigentes, cupos));
    }

    private static Asistencia asistencia(FilaAsistenciaBi total, List<FilaDiaSemana> porDia,
            List<FilaConvocatoria> filas) {
        return new Asistencia(porcentaje(total.asistieron(), total.esperados()), total.dias(),
                suma(filas, FilaConvocatoria::becasEnRiesgo), dias(porDia));
    }

    /** Siempre los 7 dias de la semana (lunes a domingo); la tasa es null donde nunca hubo registro. */
    private static List<AsistenciaDia> dias(List<FilaDiaSemana> filas) {
        List<AsistenciaDia> dias = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            final int actual = dia;
            Double tasa = filas.stream().filter(f -> f.dia() == actual).findFirst()
                    .map(f -> porcentaje(f.asistieron(), f.esperados())).orElse(null);
            dias.add(new AsistenciaDia(dia, tasa));
        }
        return dias;
    }

    private static List<GestionTendencia> tendencia(List<FilaGestion> filas, UUID seleccionada) {
        List<GestionTendencia> serie = new ArrayList<>();
        FilaGestion anterior = null;
        for (FilaGestion f : filas) {
            serie.add(new GestionTendencia(f.codigo(), f.etiqueta(), f.postulaciones(), f.adjudicadas(),
                    anterior == null ? null : variacion(f.postulaciones(), anterior.postulaciones()),
                    f.codigo().equals(seleccionada)));
            anterior = f;
        }
        return serie;
    }

    private static long suma(List<FilaConvocatoria> filas, java.util.function.ToLongFunction<FilaConvocatoria> campo) {
        return filas.stream().mapToLong(campo).sum();
    }

    /** `parte` sobre `total` en porcentaje con un decimal; null si `total` es cero. */
    static Double porcentaje(long parte, long total) {
        return total <= 0 ? null : Math.round(parte * 1000.0 / total) / 10.0;
    }

    /** Variacion porcentual de `actual` contra `anterior`; null si el anterior es cero (no hay base de comparacion). */
    static Double variacion(long actual, long anterior) {
        return anterior <= 0 ? null : Math.round((actual - anterior) * 1000.0 / anterior) / 10.0;
    }
}
