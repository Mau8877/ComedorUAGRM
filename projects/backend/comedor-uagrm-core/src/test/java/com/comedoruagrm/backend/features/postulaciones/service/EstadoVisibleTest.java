package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;

/** Estado visible de una postulacion (CU04): todas las combinaciones de los siete estados con y sin publicar. */
class EstadoVisibleTest {

    @ParameterizedTest
    @EnumSource(EstadoPostulacion.class)
    void calcular_conResultadosPublicados_devuelveSiempreElEstadoReal(EstadoPostulacion real) {
        assertThat(EstadoVisible.calcular(real, true)).isEqualTo(real);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPostulacion.class, names = {"adjudicada", "no_adjudicada"}, mode = EnumSource.Mode.EXCLUDE)
    void calcular_sinPublicar_losEstadosQueNoSonDecisionFinalSeVenDeInmediato(EstadoPostulacion real) {
        assertThat(EstadoVisible.calcular(real, false)).isEqualTo(real);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPostulacion.class, names = {"adjudicada", "no_adjudicada"})
    void calcular_sinPublicar_laDecisionFinalSeVeComoCalificada(EstadoPostulacion real) {
        assertThat(EstadoVisible.calcular(real, false)).isEqualTo(EstadoPostulacion.calificada);
    }

    @ParameterizedTest
    @EnumSource(EstadoPostulacion.class)
    void ocultaFila_conResultadosPublicados_nuncaOculta(EstadoPostulacion nuevo) {
        assertThat(EstadoVisible.ocultaFila(nuevo, true)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPostulacion.class, names = {"adjudicada", "no_adjudicada"}, mode = EnumSource.Mode.EXCLUDE)
    void ocultaFila_sinPublicar_noOcultaLoQueNoEsDecisionFinal(EstadoPostulacion nuevo) {
        assertThat(EstadoVisible.ocultaFila(nuevo, false)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPostulacion.class, names = {"adjudicada", "no_adjudicada"})
    void ocultaFila_sinPublicar_ocultaLaDecisionFinal(EstadoPostulacion nuevo) {
        assertThat(EstadoVisible.ocultaFila(nuevo, false)).isTrue();
    }

    @Test
    void visibleYFilaOculta_sonCoherentes_paraCadaEstado() {
        for (EstadoPostulacion estado : EstadoPostulacion.values()) {
            boolean seVeComoOtro = EstadoVisible.calcular(estado, false) != estado;

            assertThat(EstadoVisible.ocultaFila(estado, false)).isEqualTo(seVeComoOtro);
        }
    }
}
