package com.comedoruagrm.backend.features.convocatorias.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ConvocatoriaRequestTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabrica.close();
    }

    private ConvocatoriaRequest valida() {
        return new ConvocatoriaRequest("Beca Comedor 2026-II", UUID.randomUUID(), LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), 100, new BigDecimal("40.50"), 120,
                List.of(new DocumentoRequeridoRequest(UUID.randomUUID(), true)));
    }

    private Set<String> camposInvalidos(ConvocatoriaRequest request) {
        return validador.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void valida_noTieneErrores() {
        assertThat(validador.validate(valida())).isEmpty();
    }

    @Test
    void opcionalesNulos_sonValidos() {
        ConvocatoriaRequest base = valida();
        ConvocatoriaRequest sinOpcionales = new ConvocatoriaRequest(base.nombre(), base.gestionCodigo(),
                base.fechaApertura(), base.fechaCierre(), null, null, null, base.documentos());

        assertThat(validador.validate(sinOpcionales)).isEmpty();
    }

    @Test
    void nombreVacioOMuyLargo_esInvalido() {
        ConvocatoriaRequest base = valida();

        assertThat(camposInvalidos(new ConvocatoriaRequest(" ", base.gestionCodigo(), base.fechaApertura(),
                base.fechaCierre(), 1, null, null, base.documentos()))).containsExactly("nombre");
        assertThat(camposInvalidos(new ConvocatoriaRequest("x".repeat(151), base.gestionCodigo(),
                base.fechaApertura(), base.fechaCierre(), 1, null, null, base.documentos())))
                .containsExactly("nombre");
    }

    @Test
    void cuposCeroOCantidadDeFinalistasNegativa_sonInvalidos() {
        ConvocatoriaRequest base = valida();

        assertThat(camposInvalidos(new ConvocatoriaRequest(base.nombre(), base.gestionCodigo(),
                base.fechaApertura(), base.fechaCierre(), 0, null, -1, base.documentos())))
                .containsExactlyInAnyOrder("cupos", "cantidadFinalistas");
    }

    @Test
    void puntajeMinimoFueraDeRangoOConMuchosDecimales_esInvalido() {
        ConvocatoriaRequest base = valida();

        for (String puntaje : List.of("-1", "100.01", "40.123")) {
            assertThat(camposInvalidos(new ConvocatoriaRequest(base.nombre(), base.gestionCodigo(),
                    base.fechaApertura(), base.fechaCierre(), 1, new BigDecimal(puntaje), 1, base.documentos())))
                    .as(puntaje).containsExactly("puntajeMinimo");
        }
    }

    @Test
    void extremosDelPuntajeMinimo_sonValidos() {
        ConvocatoriaRequest base = valida();

        for (String puntaje : List.of("0", "100")) {
            assertThat(validador.validate(new ConvocatoriaRequest(base.nombre(), base.gestionCodigo(),
                    base.fechaApertura(), base.fechaCierre(), 1, new BigDecimal(puntaje), 1, base.documentos())))
                    .as(puntaje).isEmpty();
        }
    }

    @Test
    void sinDocumentos_esInvalido() {
        ConvocatoriaRequest base = valida();

        assertThat(camposInvalidos(new ConvocatoriaRequest(base.nombre(), base.gestionCodigo(),
                base.fechaApertura(), base.fechaCierre(), 1, null, 1, List.of()))).containsExactly("documentos");
    }

    @Test
    void faltanLasFechasOLaGestion_esInvalido() {
        ConvocatoriaRequest base = valida();

        assertThat(camposInvalidos(new ConvocatoriaRequest(base.nombre(), null, null, null, 1, null, 1,
                base.documentos()))).containsExactlyInAnyOrder("gestionCodigo", "fechaApertura", "fechaCierre");
    }

    @Test
    void documentoSinTipoOSinObligatorio_esInvalido() {
        ConvocatoriaRequest base = valida();

        assertThat(camposInvalidos(new ConvocatoriaRequest(base.nombre(), base.gestionCodigo(),
                base.fechaApertura(), base.fechaCierre(), 1, null, 1,
                List.of(new DocumentoRequeridoRequest(null, null)))))
                .containsExactlyInAnyOrder("documentos[0].tipoDocumentoCodigo", "documentos[0].obligatorio");
    }

    @Test
    void gestion_etiquetaVaciaOMuyLarga_oSinFechas_esInvalida() {
        assertThat(validador.validate(new GestionAcademicaRequest("2026-II", LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 20)))).isEmpty();
        assertThat(validador.validate(new GestionAcademicaRequest("", null, null))).hasSize(3);
        assertThat(validador.validate(new GestionAcademicaRequest("x".repeat(51), LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 20)))).hasSize(1);
    }
}
