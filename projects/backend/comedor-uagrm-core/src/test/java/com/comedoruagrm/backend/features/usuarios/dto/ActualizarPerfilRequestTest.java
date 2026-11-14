package com.comedoruagrm.backend.features.usuarios.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class ActualizarPerfilRequestTest {

    private static final ValidatorFactory FABRICA = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void cerrar() {
        FABRICA.close();
    }

    @Test
    void validar_sinNingunCampo_esValido() {
        assertThat(violaciones(new ActualizarPerfilRequest(null, null, null))).isEmpty();
    }

    @Test
    void validar_conTelefonoEnBlancoOLargo_loMarca() {
        assertThat(violaciones(new ActualizarPerfilRequest(null, "  ", null))).contains("telefono");
        assertThat(violaciones(new ActualizarPerfilRequest(null, "7".repeat(21), null))).contains("telefono");
        assertThat(violaciones(new ActualizarPerfilRequest(null, "70000001", null))).isEmpty();
    }

    @Test
    void validar_conCorreoEnBlancoMalFormadoOLargo_loMarca() {
        assertThat(violaciones(new ActualizarPerfilRequest(" ", null, null))).contains("correo");
        assertThat(violaciones(new ActualizarPerfilRequest("no-es-correo", null, null))).contains("correo");
        assertThat(violaciones(new ActualizarPerfilRequest("a".repeat(150) + "@x.bo", null, null))).contains("correo");
        assertThat(violaciones(new ActualizarPerfilRequest("nuevo@uagrm.edu.bo", null, "clave"))).isEmpty();
    }

    private Set<String> violaciones(ActualizarPerfilRequest request) {
        return FABRICA.getValidator().validate(request).stream()
                .map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
