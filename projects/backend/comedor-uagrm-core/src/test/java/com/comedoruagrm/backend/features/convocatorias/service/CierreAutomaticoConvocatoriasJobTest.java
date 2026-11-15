package com.comedoruagrm.backend.features.convocatorias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.features.convocatorias.ConvocatoriaPruebas;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaRepository;

class CierreAutomaticoConvocatoriasJobTest {

    private final ConvocatoriaRepository convocatoriaRepository = mock(ConvocatoriaRepository.class);
    private final ConvocatoriaEstadoService estadoService = mock(ConvocatoriaEstadoService.class);
    private final CierreAutomaticoConvocatoriasJob job =
            new CierreAutomaticoConvocatoriasJob(convocatoriaRepository, estadoService);

    private Convocatoria vencida(long id) {
        return ConvocatoriaPruebas.convocatoria(id, ConvocatoriaPruebas.gestion(id, "G" + id),
                EstadoConvocatoria.ABIERTA);
    }

    private ResultadoAuditado<Void> cerrada(long id) {
        return new ResultadoAuditado<>(null, null, null, id, Map.of(), true);
    }

    @Test
    void cerrarVencidas_cierraCadaUnaYDevuelveCuantas() {
        when(convocatoriaRepository.findByEstadoAndFechaCierreLessThanEqual(
                eq(EstadoConvocatoria.ABIERTA), any(Instant.class))).thenReturn(List.of(vencida(1L), vencida(2L)));
        when(estadoService.cerrarVencida(1L)).thenReturn(cerrada(1L));
        when(estadoService.cerrarVencida(2L)).thenReturn(cerrada(2L));

        assertThat(job.cerrarVencidas()).isEqualTo(2);
        verify(estadoService).cerrarVencida(1L);
        verify(estadoService).cerrarVencida(2L);
    }

    @Test
    void cerrarVencidas_siUnaFalla_lasDemasSeCierranIgual() {
        when(convocatoriaRepository.findByEstadoAndFechaCierreLessThanEqual(
                eq(EstadoConvocatoria.ABIERTA), any(Instant.class))).thenReturn(List.of(vencida(1L), vencida(2L)));
        when(estadoService.cerrarVencida(1L)).thenThrow(new IllegalStateException("fallo de prueba"));
        when(estadoService.cerrarVencida(2L)).thenReturn(cerrada(2L));

        assertThat(job.cerrarVencidas()).isEqualTo(1);
        verify(estadoService).cerrarVencida(2L);
    }

    @Test
    void cerrarVencidas_noContarLasQueYaNoHacianFalta() {
        when(convocatoriaRepository.findByEstadoAndFechaCierreLessThanEqual(
                eq(EstadoConvocatoria.ABIERTA), any(Instant.class))).thenReturn(List.of(vencida(1L)));
        when(estadoService.cerrarVencida(1L)).thenReturn(new ResultadoAuditado<>(null, null, null, 1L, Map.of(), false));

        assertThat(job.cerrarVencidas()).isZero();
    }

    @Test
    void cerrarVencidas_sinVencidas_noHaceNada() {
        when(convocatoriaRepository.findByEstadoAndFechaCierreLessThanEqual(
                eq(EstadoConvocatoria.ABIERTA), any(Instant.class))).thenReturn(List.of());

        assertThat(job.cerrarVencidas()).isZero();
    }
}
