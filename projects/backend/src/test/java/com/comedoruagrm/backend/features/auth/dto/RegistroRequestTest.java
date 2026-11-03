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
        RegistroRequest vacio = new RegistroRequest("", " ", null, "", null, "", " ", "", "");

        Set<String> campos = nombresDeCampo(violaciones(vacio));

        assertThat(campos).containsExactlyInAnyOrder(
                "nombres", "apellidos", "ci", "registroUniversitario", "carreraCodigo", "telefono",
                "correo", "contrasena", "confirmacionContrasena");
    }

    @Test
    void validar_conLargosExcedidos_marcaLosCamposLargos() {
        RegistroRequest largo = new RegistroRequest("a".repeat(101), "b".repeat(101), "1".repeat(21),
                "219012345", UUID.randomUUID(), "7".repeat(21), "ana@uagrm.edu.bo", "secreta1", "secreta1");

        assertThat(nombresDeCampo(violaciones(largo))).containsExactlyInAnyOrder(
                "nombres", "apellidos", "ci", "telefono");
    }

    @Test
    void validar_conRegistroUniversitarioInvalido_marcaElCampo() {
        for (String invalido : new String[] {"21901234", "2190123456", "019012345", "21901234a", "219 12345"}) {
            RegistroRequest request = con(invalido, "ana@uagrm.edu.bo", "secreta1", "secreta1");

            assertThat(nombresDeCampo(violaciones(request)))
                    .as("registro universitario %s", invalido)
                    .containsExactly("registroUniversitario");
        }
    }

    @Test
    void validar_conNueveDigitosSinCeroInicial_esValido() {
        for (String valido : new String[] {"100000000", "219012345", "999999999"}) {
            RegistroRequest request = con(valido, "ana@uagrm.edu.bo", "secreta1", "secreta1");

            assertThat(violaciones(request)).as("registro universitario %s", valido).isEmpty();
        }
    }

    @Test
    void validar_conUnCorreoSinFormato_marcaElCampo() {
        assertThat(nombresDeCampo(violaciones(con("219012345", "no-es-un-correo", "secreta1", "secreta1"))))
                .containsExactly("correo");
    }

    @Test
    void validar_conUnaContrasenaDeMenosDeSeisCaracteres_marcaElCampo() {
        assertThat(nombresDeCampo(violaciones(con("219012345", "ana@uagrm.edu.bo", "12345", "12345"))))
                .containsExactly("contrasena");
    }

    @Test
    void validar_conUnaContrasenaDeSeisCaracteres_esValida() {
        assertThat(violaciones(con("219012345", "ana@uagrm.edu.bo", "123456", "123456"))).isEmpty();
    }

    @Test
    void validar_conUnaContrasenaMasLargaQueElLimiteDeBcrypt_marcaElCampo() {
        String larga = "a".repeat(73);

        assertThat(nombresDeCampo(violaciones(con("219012345", "ana@uagrm.edu.bo", larga, larga))))
                .containsExactly("contrasena");
    }

    @Test
    void validar_conUnaConfirmacionQueNoCoincide_marcaLaConfirmacion() {
        assertThat(nombresDeCampo(violaciones(con("219012345", "ana@uagrm.edu.bo", "secreta1", "secreta2"))))
                .containsExactly("confirmacionCoincide");
    }

    private RegistroRequest completo() {
        return con("219012345", "ana@uagrm.edu.bo", "secreta1", "secreta1");
    }

    private RegistroRequest con(String registroUniversitario, String correo, String contrasena, String confirmacion) {
        return new RegistroRequest("Ana", "Perez", "1234567", registroUniversitario, UUID.randomUUID(),
                "70000001", correo, contrasena, confirmacion);
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
