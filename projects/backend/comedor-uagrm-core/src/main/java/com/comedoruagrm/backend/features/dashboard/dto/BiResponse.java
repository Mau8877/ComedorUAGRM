package com.comedoruagrm.backend.features.dashboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.ConvocatoriaResumen;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.GestionResumen;

/**
 * Analisis BI del dashboard del Directivo (CU12): lo que va mas alla de los cuatro indicadores y las series por mes.
 *
 * * Todo sale de las vistas `mv_bi_*` refrescadas por lote; ninguna cifra lleva datos personales.
 * * Los porcentajes van de 0 a 100 con un decimal; un porcentaje es null cuando su denominador es cero (no hay
 * * nada que comparar), nunca 0 inventado.
 * * `convocatorias` y `tendencia` ignoran el filtro de convocatoria a proposito: son las comparaciones. La que esta
 * * seleccionada viene marcada.
 * * `sinDatos` es verdadero si no hay postulaciones, finalistas ni adjudicadas en la seleccion.
 */
public record BiResponse(GestionResumen gestion, ConvocatoriaResumen convocatoria, Embudo embudo,
        List<Facultad> facultades, List<ConvocatoriaComparada> convocatorias, List<TramoPuntaje> puntaje,
        Double puntajePromedio, Cobertura cobertura, Asistencia asistencia, List<GestionTendencia> tendencia,
        Instant actualizadoEn, boolean sinDatos) {

    /** El camino de una postulacion: cada paso es un subconjunto del anterior (salvo las becas, que es el padron). */
    public record Embudo(long postulaciones, long precalificadas, long finalistas, long adjudicadas,
            long becasVigentes) {
    }

    /** Una facultad: cuanto postula y cuanto logra. `porcentajePostulaciones` es su parte del total. */
    public record Facultad(String nombre, long postulaciones, long adjudicadas, Double porcentajePostulaciones,
            Double tasaAdjudicacion, List<Carrera> carreras) {
    }

    /** Una carrera dentro de su facultad. */
    public record Carrera(String nombre, long postulaciones, long adjudicadas) {
    }

    /** Una convocatoria de la gestion; la variacion es contra la anterior de la gestion (null la primera). */
    public record ConvocatoriaComparada(UUID codigo, String nombre, Integer cupos, long postulaciones, long finalistas,
            long adjudicadas, Double tasaAdjudicacion, Double puntajePromedio, Double variacionPostulaciones,
            boolean seleccionada) {
    }

    /** Un tramo de 10 puntos del puntaje de la IA; `hasta` es inclusive solo en el ultimo tramo (70). */
    public record TramoPuntaje(int desde, int hasta, long cantidad) {
    }

    /** El padron de becas frente a los cupos. `porcentaje` es vigentes sobre cupos (null si no hay cupos). */
    public record Cobertura(long cupos, long becasVigentes, long becasSuspendidas, long becasBaja, long reemplazos,
            Double porcentaje) {
    }

    /** La asistencia de la seleccion: tasa, dias con registro, becas en riesgo y la tasa por dia de la semana. */
    public record Asistencia(Double tasaPromedio, long diasConRegistro, long becasEnRiesgo,
            List<AsistenciaDia> porDiaSemana) {
    }

    /** `dia` es el dia ISO de la semana (1 = lunes ... 7 = domingo). */
    public record AsistenciaDia(int dia, Double tasa) {
    }

    /** Una gestion en la serie historica; la variacion es contra la gestion anterior (null la primera). */
    public record GestionTendencia(UUID codigo, String etiqueta, long postulaciones, long adjudicadas,
            Double variacionPostulaciones, boolean seleccionada) {
    }
}
