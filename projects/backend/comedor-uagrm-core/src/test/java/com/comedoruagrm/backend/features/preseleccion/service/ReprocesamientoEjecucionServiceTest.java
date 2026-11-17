package com.comedoruagrm.backend.features.preseleccion.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.comedoruagrm.backend.features.preseleccion.dto.ModeloAplicable;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository.LoteFila;
import com.comedoruagrm.backend.features.preseleccion.service.PrecalificacionService.Resultado;

class ReprocesamientoEjecucionServiceTest {

    private final ReprocesamientoConsultaRepository consulta = mock(ReprocesamientoConsultaRepository.class);
    private final PrecalificacionService precalificacion = mock(PrecalificacionService.class);
    private final ReprocesamientoEjecucionService service = new ReprocesamientoEjecucionService(consulta,
            precalificacion);

    private final ModeloAplicable modelo = new ModeloAplicable(9L, "v1.4", Map.of("dependientes", BigDecimal.TEN));

    private LoteFila lote(String estado) {
        return new LoteFila(21L, UUID.randomUUID(), 4L, UUID.randomUUID(), 9L, "v1.4", estado, 0, 0, 0, null, null);
    }

    private void preparar(String estado, Long... ids) {
        when(consulta.loteDe(21L)).thenReturn(Optional.of(lote(estado)));
        when(precalificacion.aplicable(9L, "v1.4")).thenReturn(modelo);
        when(consulta.porReprocesar(4L, 9L)).thenReturn(List.of(ids));
    }

    @Test
    void ejecutar_recalculaCadaPostulacionCuentaLasFallidasYTerminaCompletado() {
        preparar("PENDIENTE", 1L, 2L, 3L, 4L);
        when(precalificacion.recalcular(1L, modelo)).thenReturn(Resultado.RECALCULADA);
        when(precalificacion.recalcular(2L, modelo)).thenReturn(Resultado.NO_CALCULADA);
        when(precalificacion.recalcular(3L, modelo)).thenThrow(new IllegalStateException("base"));
        when(precalificacion.recalcular(4L, modelo)).thenReturn(Resultado.OMITIDA);

        service.ejecutar(21L);

        InOrder orden = inOrder(consulta);
        orden.verify(consulta).marcarEnProceso(21L, 4);
        verify(consulta, times(2)).sumarAvance(21L, false); // recalculada y omitida
        verify(consulta, times(2)).sumarAvance(21L, true); // no calculada y excepcion
        orden.verify(consulta).terminar(21L, "COMPLETADO");
    }

    @Test
    void ejecutar_siLaIaNoEstaDisponible_cortaElLoteComoFallidoSinContarLaQueNoSeCalculo() {
        preparar("PENDIENTE", 1L, 2L, 3L);
        when(precalificacion.recalcular(1L, modelo)).thenReturn(Resultado.RECALCULADA);
        when(precalificacion.recalcular(2L, modelo)).thenReturn(Resultado.IA_NO_DISPONIBLE);

        service.ejecutar(21L);

        verify(consulta, times(1)).sumarAvance(anyLong(), anyBoolean());
        verify(precalificacion, never()).recalcular(3L, modelo);
        verify(consulta).terminar(21L, "FALLIDO");
    }

    @Test
    void ejecutar_sinNadaPorRecalcularAlArrancar_terminaCompletadoConCero() {
        preparar("PENDIENTE");

        service.ejecutar(21L);

        verify(consulta).marcarEnProceso(21L, 0);
        verify(consulta).terminar(21L, "COMPLETADO");
    }

    @Test
    void ejecutar_unLoteQueYaNoEstaPendiente_noHaceNada() {
        when(consulta.loteDe(21L)).thenReturn(Optional.of(lote("EN_PROCESO")));

        service.ejecutar(21L);

        verify(consulta, never()).marcarEnProceso(anyLong(), anyInt());
        verify(precalificacion, never()).recalcular(anyLong(), any());
    }

    @Test
    void ejecutar_unLoteInexistente_noHaceNada() {
        when(consulta.loteDe(21L)).thenReturn(Optional.empty());

        service.ejecutar(21L);

        verify(consulta, never()).terminar(anyLong(), any());
    }

    @Test
    void ejecutar_siFallaLaConsultaDeLasPendientes_dejaElLoteFallidoYNoLanza() {
        when(consulta.loteDe(21L)).thenReturn(Optional.of(lote("PENDIENTE")));
        when(precalificacion.aplicable(9L, "v1.4")).thenReturn(modelo);
        when(consulta.porReprocesar(4L, 9L)).thenThrow(new IllegalStateException("base caida"));

        service.ejecutar(21L);

        verify(consulta).terminar(21L, "FALLIDO");
    }

    @Test
    void alCrearseElLote_ejecutaElLoteDelEvento() {
        when(consulta.loteDe(21L)).thenReturn(Optional.empty());

        service.alCrearseElLote(new LoteReprocesamientoCreado(21L));

        verify(consulta).loteDe(21L);
    }
}
