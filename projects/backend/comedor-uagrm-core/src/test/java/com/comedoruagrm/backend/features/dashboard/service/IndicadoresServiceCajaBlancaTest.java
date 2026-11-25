package com.comedoruagrm.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.ConvocatoriaDatos;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.FilaAsistenciaTotal;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.GestionDatos;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.TotalesFila;

/**
 * Pruebas de caja blanca de CU-12 (capitulo 9, 9.3.2): un test por camino basico factible de
 * `IndicadoresService.obtener` (C1 a C5). El sexto camino no es factible (los nodos 4 y 12
 * evaluan la misma variable).
 */
class IndicadoresServiceCajaBlancaTest {

    private final IndicadoresRepository repositorio = mock(IndicadoresRepository.class);
    private final FiltrosDashboardService filtros = mock(FiltrosDashboardService.class);
    private final IndicadoresService servicio = new IndicadoresService(repositorio, filtros);
    private final GestionDatos gestion = new GestionDatos(7, UUID.randomUUID(), "2026-II");
    private final ConvocatoriaDatos convocatoria = new ConvocatoriaDatos(70, UUID.randomUUID(), "Beca 2026",
            gestion);

    private void totales(long postulaciones, long finalistas, long adjudicadas) {
        when(repositorio.totales(anyLong(), any())).thenReturn(new TotalesFila(postulaciones, finalistas, adjudicadas));
        when(repositorio.asistenciaTotal(anyLong(), any())).thenReturn(new FilaAsistenciaTotal(0, 0));
    }

    @Test
    void obtener_C1_sinGestion_devuelveIndicadoresVaciosConSinDatos() {
        when(filtros.gestionVigente()).thenReturn(Optional.empty());

        IndicadoresResponse r = servicio.obtener(null, null);

        assertThat(r.gestion()).isNull();
        assertThat(r.convocatoria()).isNull();
        assertThat(r.totales().postulacionesRecibidas()).isZero();
        assertThat(r.sinDatos()).isTrue();
    }

    @Test
    void obtener_C2_conConvocatoriaYTodoEnCero_sinDatosEsVerdadero() {
        when(repositorio.convocatoria(convocatoria.codigo())).thenReturn(Optional.of(convocatoria));
        totales(0, 0, 0);

        IndicadoresResponse r = servicio.obtener(null, convocatoria.codigo());

        assertThat(r.convocatoria().codigo()).isEqualTo(convocatoria.codigo());
        assertThat(r.sinDatos()).isTrue();
    }

    @Test
    void obtener_C3_sinConvocatoria_indicadoresDeTodasLasConvocatoriasDeLaGestion() {
        when(repositorio.gestion(gestion.codigo())).thenReturn(Optional.of(gestion));
        totales(0, 0, 0);

        IndicadoresResponse r = servicio.obtener(gestion.codigo(), null);

        assertThat(r.gestion().codigo()).isEqualTo(gestion.codigo());
        assertThat(r.convocatoria()).isNull();
        assertThat(r.sinDatos()).isTrue();
    }

    @Test
    void obtener_C4_conPostulaciones_sinDatosEsFalso() {
        when(repositorio.convocatoria(convocatoria.codigo())).thenReturn(Optional.of(convocatoria));
        totales(10, 0, 0);

        assertThat(servicio.obtener(null, convocatoria.codigo()).sinDatos()).isFalse();
    }

    @Test
    void obtener_C5_conFinalistas_sinDatosEsFalso() {
        when(repositorio.convocatoria(convocatoria.codigo())).thenReturn(Optional.of(convocatoria));
        totales(0, 4, 0);

        assertThat(servicio.obtener(null, convocatoria.codigo()).sinDatos()).isFalse();
    }
}
