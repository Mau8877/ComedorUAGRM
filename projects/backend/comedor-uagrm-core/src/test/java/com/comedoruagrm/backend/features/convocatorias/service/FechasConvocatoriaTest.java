package com.comedoruagrm.backend.features.convocatorias.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class FechasConvocatoriaTest {

    @Test
    void aperturaDesde_empiezaA_las0000DeLaPaz() {
        // La Paz es UTC-4: las 00:00 locales son las 04:00 UTC.
        assertThat(FechasConvocatoria.aperturaDesde(LocalDate.of(2026, 8, 1)))
                .isEqualTo(Instant.parse("2026-08-01T04:00:00Z"));
    }

    @Test
    void cierreHasta_terminaA_las235959DeLaPaz() {
        assertThat(FechasConvocatoria.cierreHasta(LocalDate.of(2026, 8, 31)))
                .isEqualTo(Instant.parse("2026-09-01T03:59:59Z"));
    }

    @Test
    void aFecha_unCierreAlFinalDelDia_siguePerteneciendoAEseDia() {
        // En UTC ese instante ya es el 1 de septiembre, pero en Bolivia sigue siendo el 31 de agosto.
        Instant cierre = FechasConvocatoria.cierreHasta(LocalDate.of(2026, 8, 31));

        assertThat(FechasConvocatoria.aFecha(cierre)).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void idaYVuelta_devuelveLaMismaFecha() {
        LocalDate fecha = LocalDate.of(2026, 12, 31);

        assertThat(FechasConvocatoria.aFecha(FechasConvocatoria.aperturaDesde(fecha))).isEqualTo(fecha);
        assertThat(FechasConvocatoria.aFecha(FechasConvocatoria.cierreHasta(fecha))).isEqualTo(fecha);
    }
}
