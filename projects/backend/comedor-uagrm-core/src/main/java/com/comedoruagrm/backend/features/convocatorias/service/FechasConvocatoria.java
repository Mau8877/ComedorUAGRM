package com.comedoruagrm.backend.features.convocatorias.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Conversion entre la fecha de calendario que ve el Administrador (hora de Bolivia) y el instante que se
 * guarda en `convocatoria` (`timestamptz`).
 *
 * * La apertura rige desde las 00:00 del dia y el cierre hasta las 23:59:59 del dia, en `America/La_Paz`.
 * * Todo el modulo usa esta clase para que apertura y cierre signifiquen siempre lo mismo.
 */
public final class FechasConvocatoria {

    /** Zona horaria de Bolivia. */
    public static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    private FechasConvocatoria() {
    }

    /** El instante en que empieza el dia de apertura (00:00:00 en La Paz). */
    public static Instant aperturaDesde(LocalDate fecha) {
        return fecha.atStartOfDay(ZONA).toInstant();
    }

    /** El instante en que termina el dia de cierre (23:59:59 en La Paz). */
    public static Instant cierreHasta(LocalDate fecha) {
        return fecha.atTime(LocalTime.of(23, 59, 59)).atZone(ZONA).toInstant();
    }

    /** La fecha de calendario (en La Paz) de un instante guardado. */
    public static LocalDate aFecha(Instant instante) {
        return instante.atZone(ZONA).toLocalDate();
    }
}
