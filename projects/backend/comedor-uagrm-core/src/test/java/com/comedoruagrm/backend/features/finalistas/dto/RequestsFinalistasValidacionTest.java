package com.comedoruagrm.backend.features.finalistas.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Bean Validation de los requests de finalistas (CU21): los campos vacios caen en ERR_SYS_01 (400). */
class RequestsFinalistasValidacionTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void notificar_completoEsValido() {
        var request = new NotificarFinalistasRequest(List.of(UUID.randomUUID()), LocalDate.now().plusDays(3),
                "DUBSS, 8:00 a 12:00", "CI y certificado");

        assertThat(validador.validate(request)).isEmpty();
    }

    @Test
    void notificar_sinFinalistasPlazoLugarNiDocumentos_senalaCadaCampo() {
        var request = new NotificarFinalistasRequest(List.of(), null, " ", "");

        assertThat(validador.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("finalistas", "plazoEntrega", "lugarHorario", "documentos");
    }

    @Test
    void notificar_lugarYDocumentosNoPasanDeSuLimite() {
        var request = new NotificarFinalistasRequest(List.of(UUID.randomUUID()), LocalDate.now().plusDays(1),
                "x".repeat(301), "y".repeat(501));

        assertThat(validador.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("lugarHorario", "documentos");
    }

    @Test
    void entrega_sinNadaEsValida_peroLasObservacionesTienenLimite() {
        assertThat(validador.validate(new RegistrarEntregaRequest(null, null))).isEmpty();
        assertThat(validador.validate(new RegistrarEntregaRequest(null, "z".repeat(501)))).hasSize(1);
    }
}
