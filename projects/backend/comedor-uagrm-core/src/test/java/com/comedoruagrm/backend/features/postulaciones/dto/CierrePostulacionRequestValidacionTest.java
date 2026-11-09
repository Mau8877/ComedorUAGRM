package com.comedoruagrm.backend.features.postulaciones.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Bean Validation del motivo de un cierre (CU20): obligatorio y de hasta 500 caracteres (ERR_SYS_01). */
class CierrePostulacionRequestValidacionTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void motivoDeExactamente500Caracteres_esValido() {
        assertThat(validador.validate(new CierrePostulacionRequest("x".repeat(500)))).isEmpty();
    }

    @Test
    void motivoDe501Caracteres_senalaElCampo() {
        assertThat(validador.validate(new CierrePostulacionRequest("x".repeat(501))))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("motivo");
    }

    @Test
    void motivoNuloVacioOSoloEspacios_senalaElCampo() {
        for (String motivo : new String[] {null, "", "   "}) {
            assertThat(validador.validate(new CierrePostulacionRequest(motivo)))
                    .extracting(v -> v.getPropertyPath().toString()).containsExactly("motivo");
        }
    }
}
