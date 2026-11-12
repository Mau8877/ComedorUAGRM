package com.comedoruagrm.backend.features.postulaciones.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Bean Validation de los DTOs de entrada de la adjudicacion (CU05), que caen en ERR_SYS_01. */
class NoAdjudicarRequestValidacionTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void motivoDeExactamente500Caracteres_esValido() {
        assertThat(validador.validate(new NoAdjudicarRequest("x".repeat(500)))).isEmpty();
    }

    @Test
    void motivoDe501Caracteres_senalaElCampo() {
        assertThat(validador.validate(new NoAdjudicarRequest("x".repeat(501))))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("motivo");
    }

    @Test
    void motivoNuloVacioOSoloEspacios_senalaElCampo() {
        for (String motivo : new String[] {null, "", "   "}) {
            assertThat(validador.validate(new NoAdjudicarRequest(motivo)))
                    .extracting(v -> v.getPropertyPath().toString()).containsExactly("motivo");
        }
    }

    @Test
    void cantidadNula_esValida() {
        assertThat(validador.validate(new SeleccionarFinalistasRequest(null))).isEmpty();
    }

    @Test
    void cantidadUno_esValida() {
        assertThat(validador.validate(new SeleccionarFinalistasRequest(1))).isEmpty();
    }

    @Test
    void cantidadCeroONegativa_senalaElCampo() {
        for (int cantidad : new int[] {0, -1, -50}) {
            assertThat(validador.validate(new SeleccionarFinalistasRequest(cantidad)))
                    .extracting(v -> v.getPropertyPath().toString()).containsExactly("cantidad");
        }
    }
}
