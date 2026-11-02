package com.comedoruagrm.backend.features.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class RegistroRequestTest {

    private static final ValidatorFactory FABRICA = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void cerrar() {
        FABRICA.close();
    }

    @Test
    void validar_conUnRequestCompleto_noTieneViolaciones() {
        assertThat(violaciones(completo())).isEmpty();
    }

    @Test
    void validar_conCamposVacios_marcaCadaCampoObligatorio() {
        RegistroRequest vacio = new RegistroRequest("", " ", null, "", null, "");

        Set<String> campos = nombresDeCampo(violaciones(vacio));

        assertThat(campos).containsExactlyInAnyOrder(
                "nombres", "apellidos", "ci", "registroUniversitario", "carreraCodigo", "telefono");
    }

    @Test
    void validar_conLargosExcedidos_marcaLosCamposLargos() {
        RegistroRequest largo = new RegistroRequest("a".repeat(101), "b".repeat(101), "1".repeat(21),
                "219012345", UUID.randomUUID(), "7".repeat(21));

        assertThat(nombresDeCampo(violaciones(largo))).containsExactlyInAnyOrder(
                "nombres", "apellidos", "ci", "telefono");
    }

    @Test
    void validar_conRegistroUniversitarioInvalido_marcaElCampo() {
        for (String invalido : new String[] {"21901234", "2190123456", "019012345", "21901234a", "219 12345"}) {
            RegistroRequest request = new RegistroRequest("Ana", "Perez", "1234567", invalido,
                    UUID.randomUUID(), "70000001");

            assertThat(nombresDeCampo(violaciones(request)))
                    .as("registro universitario %s", invalido)
                    .containsExactly("registroUniversitario");
        }
    }

    @Test
    void validar_conNueveDigitosSinCeroInicial_esValido() {
        for (String valido : new String[] {"100000000", "219012345", "999999999"}) {
            RegistroRequest request = new RegistroRequest("Ana", "Perez", "1234567", valido,
                    UUID.randomUUID(), "70000001");

            assertThat(violaciones(request)).as("registro universitario %s", valido).isEmpty();
        }
    }

    private RegistroRequest completo() {
        return new RegistroRequest("Ana", "Perez", "1234567", "219012345", UUID.randomUUID(), "70000001");
    }

    private Set<ConstraintViolation<RegistroRequest>> violaciones(RegistroRequest request) {
        return FABRICA.getValidator().validate(request);
    }

    private Set<String> nombresDeCampo(Set<ConstraintViolation<RegistroRequest>> violaciones) {
        return violaciones.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
