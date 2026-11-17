package com.comedoruagrm.backend.features.postulaciones.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;

/** Reenviar una postulacion (CU02) reinicia la marca de revision de la precalificacion (CU03 A1). */
class PostulacionEnviarTest {

    @Test
    void reenviarUnaDevueltaReiniciaLosIntentosYLaMarcaDeRevision() throws Exception {
        var ctor = Postulacion.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Postulacion p = ctor.newInstance();
        ReflectionTestUtils.setField(p, "estado", EstadoPostulacion.devuelta);
        ReflectionTestUtils.setField(p, "intentosPrecalificacion", (short) 3);
        ReflectionTestUtils.setField(p, "precalificacionRevisionEn", Instant.parse("2026-11-01T10:00:00Z"));

        p.enviar(Instant.parse("2026-11-02T10:00:00Z"));

        assertThat(p.getEstado()).isEqualTo(EstadoPostulacion.pendiente);
        assertThat(p.getIntentosPrecalificacion()).isZero();
        assertThat(p.getPrecalificacionRevisionEn()).isNull();
    }
}
