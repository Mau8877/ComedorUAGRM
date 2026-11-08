package com.comedoruagrm.backend.features.postulaciones.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;

/** Transicion de estado de la devolucion (CU19): solo una postulacion calificada pasa a devuelta. */
class PostulacionDevolverTest {

    @Test
    void calificada_pasaADevuelta() {
        Postulacion p = nueva(EstadoPostulacion.calificada);

        p.devolver();

        assertThat(p.getEstado()).isEqualTo(EstadoPostulacion.devuelta);
    }

    @Test
    void enCualquierOtroEstado_lanzaIllegalState() {
        for (EstadoPostulacion estado : EstadoPostulacion.values()) {
            if (estado == EstadoPostulacion.calificada) {
                continue;
            }
            Postulacion p = nueva(estado);
            assertThatThrownBy(p::devolver).isInstanceOf(IllegalStateException.class);
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
