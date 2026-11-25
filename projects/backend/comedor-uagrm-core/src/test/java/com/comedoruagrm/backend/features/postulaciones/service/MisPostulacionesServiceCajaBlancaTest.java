package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.postulaciones.dto.PasoLineaTiempoResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.enums.SituacionPaso;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.FilaHistorial;

/**
 * Pruebas de caja blanca de CU-04 (capitulo 9, 9.3.2): un test por camino basico de
 * `MisPostulacionesService.lineaDeTiempo` (metodo privado y estatico, invocado por reflexion).
 */
class MisPostulacionesServiceCajaBlancaTest {

    private static final Instant T = Instant.parse("2026-11-03T15:00:00Z");

    @SuppressWarnings("unchecked")
    private static List<PasoLineaTiempoResponse> lineaDeTiempo(List<FilaHistorial> historial,
            EstadoPostulacion visible, boolean resultadosVisibles) {
        return (List<PasoLineaTiempoResponse>) ReflectionTestUtils.invokeMethod(MisPostulacionesService.class,
                "lineaDeTiempo", historial, visible, resultadosVisibles);
    }

    @Test
    void lineaDeTiempo_C1_sinFilasNiPasosPendientes_devuelveLaLineaVacia() {
        assertThat(lineaDeTiempo(List.of(), EstadoPostulacion.adjudicada, true)).isEmpty();
    }

    @Test
    void lineaDeTiempo_C2_filaOculta_noSeAgrega() {
        List<FilaHistorial> historial = List.of(new FilaHistorial("adjudicada", T, null));

        assertThat(lineaDeTiempo(historial, EstadoPostulacion.adjudicada, false)).isEmpty();
    }

    @Test
    void lineaDeTiempo_C3_filaVisible_seAgregaComoPasoCompletado() {
        List<FilaHistorial> historial = List.of(new FilaHistorial("pendiente", T, null));

        List<PasoLineaTiempoResponse> pasos = lineaDeTiempo(historial, EstadoPostulacion.adjudicada, false);

        assertThat(pasos).hasSize(1);
        assertThat(pasos.get(0).estado()).isEqualTo("pendiente");
        assertThat(pasos.get(0).fecha()).isEqualTo(T);
        assertThat(pasos.get(0).situacion()).isEqualTo(SituacionPaso.COMPLETADO);
    }

    @Test
    void lineaDeTiempo_C4_conPasosPendientes_losAgregaAlFinal() {
        List<PasoLineaTiempoResponse> pasos = lineaDeTiempo(List.of(), EstadoPostulacion.calificada, false);

        assertThat(pasos).hasSize(1);
        assertThat(pasos.get(0).fecha()).isNull();
        assertThat(pasos.get(0).situacion()).isEqualTo(SituacionPaso.PENDIENTE);
    }
}
