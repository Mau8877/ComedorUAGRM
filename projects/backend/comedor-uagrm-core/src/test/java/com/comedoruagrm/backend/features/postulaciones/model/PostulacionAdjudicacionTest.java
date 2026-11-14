package com.comedoruagrm.backend.features.postulaciones.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;

/** Transiciones de la decision final (CU05): solo una postulacion calificada pasa a adjudicada o no adjudicada. */
class PostulacionAdjudicacionTest {

    @Test
    void adjudicar_desdeCalificada_pasaAAdjudicada() {
        Postulacion p = nueva(EstadoPostulacion.calificada);

        p.adjudicar();

        assertThat(p.getEstado()).isEqualTo(EstadoPostulacion.adjudicada);
    }

    @Test
    void noAdjudicar_desdeCalificada_pasaANoAdjudicada() {
        Postulacion p = nueva(EstadoPostulacion.calificada);

        p.noAdjudicar();

        assertThat(p.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
    }

    @Test
    void adjudicar_enCualquierOtroEstado_lanzaIllegalStateYNoCambiaNada() {
        paraCadaEstadoNoCalificado(Postulacion::adjudicar);
    }

    @Test
    void noAdjudicar_enCualquierOtroEstado_lanzaIllegalStateYNoCambiaNada() {
        paraCadaEstadoNoCalificado(Postulacion::noAdjudicar);
    }

    @Test
    void unaDecisionTomada_noSePuedeTomarOtraVez() {
        Postulacion adjudicada = nueva(EstadoPostulacion.calificada);
        adjudicada.adjudicar();
        Postulacion noAdjudicada = nueva(EstadoPostulacion.calificada);
        noAdjudicada.noAdjudicar();

        assertThatThrownBy(adjudicada::noAdjudicar).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(noAdjudicada::adjudicar).isInstanceOf(IllegalStateException.class);
        assertThat(adjudicada.getEstado()).isEqualTo(EstadoPostulacion.adjudicada);
        assertThat(noAdjudicada.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
    }

    private void paraCadaEstadoNoCalificado(Consumer<Postulacion> accion) {
        for (EstadoPostulacion estado : EstadoPostulacion.values()) {
            if (estado == EstadoPostulacion.calificada) {
                continue;
            }
            Postulacion p = nueva(estado);
            assertThatThrownBy(() -> accion.accept(p)).isInstanceOf(IllegalStateException.class);
            assertThat(p.getEstado()).isEqualTo(estado);
        }
    }

    private Postulacion nueva(EstadoPostulacion estado) {
        try {
            var ctor = Postulacion.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Postulacion p = ctor.newInstance();
            ReflectionTestUtils.setField(p, "estado", estado);
            return p;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
