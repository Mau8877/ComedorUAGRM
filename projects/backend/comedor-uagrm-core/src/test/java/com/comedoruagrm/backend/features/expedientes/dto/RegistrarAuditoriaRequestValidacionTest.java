package com.comedoruagrm.backend.features.expedientes.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Bean Validation del registro de una auditoria (CU07): el resultado es obligatorio y las observaciones
 * tienen limite (ERR_SYS_01, 400). Las reglas por resultado (ERR_EXP_03 y ERR_EXP_04) las valida el service.
 */
class RegistrarAuditoriaRequestValidacionTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validado_conPuntajeAcademico_esValido() {
        var request = new RegistrarAuditoriaRequest(ResultadoAuditoria.VALIDADO, null, new BigDecimal("25.5"));

        assertThat(validador.validate(request)).isEmpty();
    }

    @Test
    void sinResultado_senalaElCampo() {
        var request = new RegistrarAuditoriaRequest(null, "Falta un documento", null);

        assertThat(validador.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("resultado");
    }

    @Test
    void observacionesDeExactamente500Caracteres_sonValidas() {
        var request = new RegistrarAuditoriaRequest(ResultadoAuditoria.OBSERVADO, "x".repeat(500), null);

        assertThat(validador.validate(request)).isEmpty();
    }

    @Test
    void observacionesDeMasDe500Caracteres_senalaElCampo() {
        var request = new RegistrarAuditoriaRequest(ResultadoAuditoria.OBSERVADO, "x".repeat(501), null);

        assertThat(validador.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("observaciones");
    }
}
