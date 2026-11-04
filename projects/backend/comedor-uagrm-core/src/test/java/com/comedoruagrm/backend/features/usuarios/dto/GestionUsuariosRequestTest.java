package com.comedoruagrm.backend.features.usuarios.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class GestionUsuariosRequestTest {

    private static final ValidatorFactory FABRICA = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void cerrar() {
        FABRICA.close();
    }

    // ---- CrearUsuarioPersonalRequest ----

    @Test
    void crear_conUnRequestCompleto_noTieneViolaciones() {
        assertThat(campos(new CrearUsuarioPersonalRequest(
                "Ana", "Perez", "1234567", "ana@uagrm.edu.bo", "DIRECTIVO"))).isEmpty();
    }

    @Test
    void crear_conCamposEnBlanco_marcaCadaCampoObligatorio() {
        assertThat(campos(new CrearUsuarioPersonalRequest("", " ", null, "", ""))).containsExactlyInAnyOrder(
                "nombres", "apellidos", "ci", "correo", "rol");
    }

    @Test
    void crear_conCorreoInvalidoYLargosExcedidos_marcaEsosCampos() {
        String largo = "x".repeat(101);

        assertThat(campos(new CrearUsuarioPersonalRequest(largo, largo, "1".repeat(21), "no-es-correo", "DIRECTIVO")))
                .containsExactlyInAnyOrder("nombres", "apellidos", "ci", "correo");
        assertThat(campos(new CrearUsuarioPersonalRequest(
                "Ana", "Perez", "1", "a".repeat(150) + "@uagrm.edu.bo", "DIRECTIVO"))).containsExactly("correo");
    }

    // ---- ActualizarUsuarioPersonalRequest ----

    @Test
    void actualizar_sinNingunCampo_esValido() {
        assertThat(campos(new ActualizarUsuarioPersonalRequest(null, null, null))).isEmpty();
    }

    @Test
    void actualizar_conUnCampoEnBlanco_loMarca() {
        assertThat(campos(new ActualizarUsuarioPersonalRequest(" ", "Perez", "ana@uagrm.edu.bo")))
                .containsExactly("nombres");
        assertThat(campos(new ActualizarUsuarioPersonalRequest(null, "", null))).contains("apellidos");
    }

    @Test
    void actualizar_conCorreoInvalidoOLargo_loMarca() {
        assertThat(campos(new ActualizarUsuarioPersonalRequest(null, null, "no-es-correo"))).containsExactly("correo");
        assertThat(campos(new ActualizarUsuarioPersonalRequest("x".repeat(101), null, null)))
                .containsExactly("nombres");
    }

    // ---- CambiarRolRequest ----

    @Test
    void cambiarRol_conRolEnBlanco_marcaElRol() {
        assertThat(campos(new CambiarRolRequest(" "))).containsExactly("rol");
        assertThat(campos(new CambiarRolRequest("DIRECTIVO"))).isEmpty();
    }

    // ---- UsuarioCreadoResponse ----

    @Test
    void usuarioCreadoResponse_de_copiaLaCuentaYAgregaLaContrasenaTemporal() {
        var cuenta = new UsuarioPersonalResponse(UUID.randomUUID(), "Ana", "Perez", "1234567",
                "ana@uagrm.edu.bo", "DIRECTIVO", true);

        UsuarioCreadoResponse creada = UsuarioCreadoResponse.de(cuenta, "Abc23456xyz9");

        assertThat(creada.codigo()).isEqualTo(cuenta.codigo());
        assertThat(creada.rol()).isEqualTo("DIRECTIVO");
        assertThat(creada.activo()).isTrue();
        assertThat(creada.contrasenaTemporal()).isEqualTo("Abc23456xyz9");
    }

    private static <T> Set<String> campos(T request) {
        return FABRICA.getValidator().validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
