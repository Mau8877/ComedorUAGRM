package com.comedoruagrm.backend.features.comunicados.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Bean Validation del request de comunicados (CU21): los campos vacios caen en ERR_SYS_01 (400). */
class PublicarComunicadoRequestValidacionTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void completoEsValido() {
        var request = new PublicarComunicadoRequest("Aviso", "Contenido", DestinatariosComunicado.TODOS, null);

        assertThat(validador.validate(request)).isEmpty();
    }

    @Test
    void sinTituloContenidoNiDestinatarios_senalaCadaCampo() {
        var request = new PublicarComunicadoRequest(" ", "", null, null);

        assertThat(validador.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("titulo", "contenido", "destinatarios");
    }

    @Test
    void tituloYContenidoNoPasanDeSuLimite() {
        var request = new PublicarComunicadoRequest("t".repeat(151), "c".repeat(5001),
                DestinatariosComunicado.FINALISTAS, null);

        assertThat(validador.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("titulo", "contenido");
    }
}
