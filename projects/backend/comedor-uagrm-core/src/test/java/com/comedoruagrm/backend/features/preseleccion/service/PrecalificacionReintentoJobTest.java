package com.comedoruagrm.backend.features.preseleccion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.service.PrecalificacionService.Resultado;

/** El reintento de las postulaciones que siguen `pendiente` (CU03: A2 y CA02, CA03). */
class PrecalificacionReintentoJobTest {

    private final PrecalificacionConsultaRepository consulta = mock(PrecalificacionConsultaRepository.class);
    private final PrecalificacionService precalificacion = mock(PrecalificacionService.class);
    private final PrecalificacionReintentoJob job = new PrecalificacionReintentoJob(consulta, precalificacion);

    @Test
    void reintentar_pideElLoteDe500DeLasMasAntiguasYCuentaLasQueQuedaronCalificadas() {
        when(consulta.pendientes(500)).thenReturn(List.of(1L, 2L, 3L));
        when(precalificacion.precalificar(1L)).thenReturn(Resultado.CALIFICADA);
        when(precalificacion.precalificar(2L)).thenReturn(Resultado.OMITIDA);
        when(precalificacion.precalificar(3L)).thenReturn(Resultado.CALIFICADA);

        assertThat(job.reintentar()).isEqualTo(2);
        verify(consulta).pendientes(PrecalificacionReintentoJob.LOTE);
        assertThat(PrecalificacionReintentoJob.LOTE).isEqualTo(500);
    }

    @Test
    void reintentar_siLaIaSigueCaidaCortaLaPasadaEnLaPrimeraYNoInsisteConLasDemas() {
        when(consulta.pendientes(500)).thenReturn(List.of(1L, 2L, 3L, 4L));
        when(precalificacion.precalificar(1L)).thenReturn(Resultado.CALIFICADA);
        when(precalificacion.precalificar(2L)).thenReturn(Resultado.IA_NO_DISPONIBLE);

        assertThat(job.reintentar()).isEqualTo(1);
        verify(precalificacion, never()).precalificar(3L);
        verify(precalificacion, never()).precalificar(4L);
    }

    @Test
    void reintentar_unaPostulacionQueLaIaNoPudoCalcularNoDetieneLasDemas() {
        when(consulta.pendientes(500)).thenReturn(List.of(1L, 2L));
        when(precalificacion.precalificar(1L)).thenReturn(Resultado.NO_CALCULADA);
        when(precalificacion.precalificar(2L)).thenReturn(Resultado.CALIFICADA);

        assertThat(job.reintentar()).isEqualTo(1);
    }

    @Test
    void reintentar_unaExcepcionInesperadaEnUnaPostulacionNoDetieneLaPasada() {
        when(consulta.pendientes(500)).thenReturn(List.of(1L, 2L));
        when(precalificacion.precalificar(1L)).thenThrow(new IllegalStateException("fallo"));
        when(precalificacion.precalificar(2L)).thenReturn(Resultado.CALIFICADA);

        assertThat(job.reintentar()).isEqualTo(1);
    }

    @Test
    void reintentar_sinPendientesNoLlamaALaIa() {
        when(consulta.pendientes(500)).thenReturn(List.of());

        assertThat(job.reintentar()).isZero();
        verify(precalificacion, never()).precalificar(anyLong());
    }
}
