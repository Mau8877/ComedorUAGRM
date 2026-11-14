package com.comedoruagrm.backend.features.actas.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Los DTOs del acta (CU08) no exponen la ruta del archivo ni datos personales. */
class ActaResponseTest {

    @Test
    void actaResponse_noExponeLaRutaDelArchivo() {
        List<String> componentes = Arrays.stream(ActaResponse.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        assertThat(componentes).containsExactly("codigo", "numero", "convocatoria", "gestion", "adjudicados",
                "generadaEn", "generadaPor");
        assertThat(componentes).noneMatch(n -> n.toLowerCase().contains("path") || n.toLowerCase().contains("ruta")
                || n.toLowerCase().contains("storage"));
    }

    @Test
    void accesoActaResponse_soloLlevaLaDireccionYSuVencimiento() {
        assertThat(Arrays.stream(AccesoActaResponse.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("url", "expiraEn");
    }
}
