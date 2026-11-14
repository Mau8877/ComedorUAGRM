package com.comedoruagrm.backend.features.postulaciones.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;

/** Transiciones de estado del cierre (CU20): solo una postulacion calificada pasa a archivada o no adjudicada. */
class PostulacionCierreTest {

    @Test
    void archivar_desdeCalificada_pasaAArchivada() {
        Postulacion p = nueva(EstadoPostulacion.calificada);

        p.archivar();

        assertThat(p.getEstado()).isEqualTo(EstadoPostulacion.archivada);
    }

    @Test
    void rechazar_desdeCalificada_pasaANoAdjudicada() {
        Postulacion p = nueva(EstadoPostulacion.calificada);

        p.rechazar();

        assertThat(p.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
    }

    @Test
    void archivar_enCualquierOtroEstado_lanzaIllegalStateYNoCambiaNada() {
        paraCadaEstadoNoCalificado(Postulacion::archivar);
    }

    @Test
    void rechazar_enCualquierOtroEstado_lanzaIllegalStateYNoCambiaNada() {
        paraCadaEstadoNoCalificado(Postulacion::rechazar);
    }

    @Test
    void unaPostulacionCerradaNoVuelveACalificadaNiSePuedeCerrarDeNuevo() {
        Postulacion archivada = nueva(EstadoPostulacion.calificada);
        archivada.archivar();
        Postulacion rechazada = nueva(EstadoPostulacion.calificada);
        rechazada.rechazar();

        assertThatThrownBy(archivada::rechazar).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(rechazada::archivar).isInstanceOf(IllegalStateException.class);
        assertThat(archivada.getEstado()).isEqualTo(EstadoPostulacion.archivada);
        assertThat(rechazada.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
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
