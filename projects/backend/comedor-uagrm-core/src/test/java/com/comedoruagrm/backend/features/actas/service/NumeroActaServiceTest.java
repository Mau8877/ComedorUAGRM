package com.comedoruagrm.backend.features.actas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository;

/** Formato, reinicio por año y zona horaria del numero de acta (CU08), con el repository simulado. */
class NumeroActaServiceTest {

    private static final Instant NOVIEMBRE_2026 = Instant.parse("2026-11-13T15:00:00Z");

    private final ActasConsultaRepository consulta = mock(ActasConsultaRepository.class);
    private final NumeroActaService service = new NumeroActaService(consulta);

    @Test
    void siguiente_elPrimeroDelAnioEs001() {
        when(consulta.mayorCorrelativoDelAnio(2026)).thenReturn(0);

        assertThat(service.siguiente(NOVIEMBRE_2026)).isEqualTo("001/2026");
    }

    @Test
    void siguiente_sumaUnoAlMayorYCompletaTresDigitos() {
        when(consulta.mayorCorrelativoDelAnio(2026)).thenReturn(41);

        assertThat(service.siguiente(NOVIEMBRE_2026)).isEqualTo("042/2026");
    }

    @Test
    void siguiente_despuesDe999SigueConCuatroDigitos() {
        when(consulta.mayorCorrelativoDelAnio(2026)).thenReturn(999);

        assertThat(service.siguiente(NOVIEMBRE_2026)).isEqualTo("1000/2026");
    }

    @Test
    void siguiente_unAnioNuevoReiniciaEn001() {
        when(consulta.mayorCorrelativoDelAnio(2026)).thenReturn(57);
        when(consulta.mayorCorrelativoDelAnio(2027)).thenReturn(0);

        assertThat(service.siguiente(NOVIEMBRE_2026)).isEqualTo("058/2026");
        assertThat(service.siguiente(Instant.parse("2027-01-05T15:00:00Z"))).isEqualTo("001/2027");
    }

    @Test
    void siguiente_tomaElAnioEnLaPazAunqueUtcYaEsteEnElSiguiente() {
        // 01:30 UTC del 1 de enero de 2027 son las 21:30 del 31 de diciembre de 2026 en La Paz (UTC-4).
        when(consulta.mayorCorrelativoDelAnio(2026)).thenReturn(12);

        assertThat(service.siguiente(Instant.parse("2027-01-01T01:30:00Z"))).isEqualTo("013/2026");
        verify(consulta).bloquearNumeracion(2026);
    }

    @Test
    void siguiente_primeroBloqueaLaNumeracionDelAnioYDespuesLeeElMayor() {
        when(consulta.mayorCorrelativoDelAnio(2026)).thenReturn(3);

        service.siguiente(NOVIEMBRE_2026);

        InOrder orden = inOrder(consulta);
        orden.verify(consulta).bloquearNumeracion(2026);
        orden.verify(consulta).mayorCorrelativoDelAnio(2026);
    }

    @Test
    void borrador_noConsultaNiBloqueaNada() {
        assertThat(service.borrador()).isEqualTo("borrador");

        verifyNoInteractions(consulta);
        verify(consulta, never()).mayorCorrelativoDelAnio(anyInt());
    }
}
