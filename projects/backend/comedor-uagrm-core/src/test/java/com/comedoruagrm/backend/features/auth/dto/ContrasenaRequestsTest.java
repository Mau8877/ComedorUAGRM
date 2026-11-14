package com.comedoruagrm.backend.features.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

/** Validaciones de los DTOs de recuperacion y cambio de contraseña (CU15). */
class ContrasenaRequestsTest {

    private static final ValidatorFactory FABRICA = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void cerrar() {
        FABRICA.close();
    }

    @Test
    void recuperar_conCorreoValido_noTieneViolaciones() {
        assertThat(violaciones(new RecuperarContrasenaRequest("ana@uagrm.edu.bo"))).isEmpty();
    }

    @Test
    void recuperar_conCorreoVacioMalFormadoOLargo_loMarca() {
        assertThat(violaciones(new RecuperarContrasenaRequest(""))).contains("correo");
        assertThat(violaciones(new RecuperarContrasenaRequest(null))).contains("correo");
        assertThat(violaciones(new RecuperarContrasenaRequest("no-es-un-correo"))).contains("correo");
        assertThat(violaciones(new RecuperarContrasenaRequest("a".repeat(150) + "@uagrm.edu.bo"))).contains("correo");
    }

    @Test
    void restablecer_conDatosValidos_noTieneViolaciones() {
        assertThat(violaciones(new RestablecerContrasenaRequest("tok", "secreta1", "secreta1"))).isEmpty();
    }

    @Test
    void restablecer_conContrasenaDe5_6y73Caracteres_soloRechazaLaCortaYLaLarga() {
        assertThat(violaciones(new RestablecerContrasenaRequest("tok", "12345", "12345"))).contains("contrasena");
        assertThat(violaciones(new RestablecerContrasenaRequest("tok", "123456", "123456"))).isEmpty();
        String larga = "a".repeat(73);
        assertThat(violaciones(new RestablecerContrasenaRequest("tok", larga, larga))).contains("contrasena");
    }

    @Test
    void restablecer_conConfirmacionDistintaOSinToken_loMarca() {
        assertThat(violaciones(new RestablecerContrasenaRequest("tok", "secreta1", "secreta2")))
                .contains("confirmacionCoincide");
        assertThat(violaciones(new RestablecerContrasenaRequest(" ", "secreta1", "secreta1"))).contains("token");
        assertThat(violaciones(new RestablecerContrasenaRequest("tok", "secreta1", ""))).contains("confirmacionContrasena");
    }

    @Test
    void cambiar_conDatosValidos_noTieneViolaciones() {
        assertThat(violaciones(new CambiarContrasenaRequest("actual", "nueva-123", "nueva-123", null))).isEmpty();
        assertThat(violaciones(new CambiarContrasenaRequest("actual", "nueva-123", "nueva-123", "refresh"))).isEmpty();
    }

    @Test
    void cambiar_sinLaActualConNuevaCortaOSinCoincidir_loMarca() {
        assertThat(violaciones(new CambiarContrasenaRequest("", "nueva-123", "nueva-123", null)))
                .contains("contrasenaActual");
        assertThat(violaciones(new CambiarContrasenaRequest("actual", "12345", "12345", null)))
                .contains("contrasenaNueva");
        assertThat(violaciones(new CambiarContrasenaRequest("actual", "nueva-123", "otra-1234", null)))
                .contains("confirmacionCoincide");
    }

    private Set<String> violaciones(Object request) {
        Set<ConstraintViolation<Object>> encontradas = FABRICA.getValidator().validate(request);
        return encontradas.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
