package com.comedoruagrm.backend.features.dashboard.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Los DTOs del dashboard (CU12) se serializan con los nombres del contrato. */
class IndicadoresResponseTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private static List<String> nombres(JsonNode nodo) {
        List<String> nombres = new ArrayList<>();
        Iterator<String> it = nodo.fieldNames();
        it.forEachRemaining(nombres::add);
        return nombres;
    }

    @Test
    void indicadores_seSerializaConLosNombresDelContrato() {
        UUID g = UUID.randomUUID();
        IndicadoresResponse r = new IndicadoresResponse(new IndicadoresResponse.GestionResumen(g, "2026-II"), null,
                new IndicadoresResponse.Totales(10, 4, 2, null),
                List.of(new IndicadoresResponse.IndicadorMensual("2026-09", 7, 3, 0, null)),
                Instant.parse("2026-11-13T06:00:00Z"), false);

        JsonNode json = mapper.valueToTree(r);

        assertThat(nombres(json)).containsExactlyInAnyOrder("gestion", "convocatoria", "totales", "porMes",
                "actualizadoEn", "sinDatos");
        assertThat(json.get("gestion").get("codigo").asText()).isEqualTo(g.toString());
        assertThat(json.get("convocatoria").isNull()).isTrue();
        assertThat(nombres(json.get("totales"))).containsExactlyInAnyOrder("postulacionesRecibidas", "finalistas",
                "becasAdjudicadas", "asistenciaPromedio");
        assertThat(json.get("totales").get("asistenciaPromedio").isNull()).isTrue();
        JsonNode mes = json.get("porMes").get(0);
        assertThat(mes.get("mes").asText()).isEqualTo("2026-09");
        assertThat(nombres(mes)).containsExactlyInAnyOrder("mes", "postulaciones", "finalistas", "adjudicadas",
                "asistencia");
        assertThat(mes.get("asistencia").isNull()).isTrue();
    }

    @Test
    void filtros_seSerializaConLosNombresDelContrato() {
        UUID c = UUID.randomUUID();
        FiltrosDashboardResponse r = new FiltrosDashboardResponse(List.of(new FiltrosDashboardResponse.GestionFiltro(
                UUID.randomUUID(), "2026-II", true,
                List.of(new FiltrosDashboardResponse.ConvocatoriaFiltro(c, "Beca 2026")))));

        JsonNode gestion = mapper.valueToTree(r).get("gestiones").get(0);

        assertThat(nombres(gestion)).containsExactlyInAnyOrder("codigo", "etiqueta", "vigente", "convocatorias");
        assertThat(gestion.get("vigente").asBoolean()).isTrue();
        assertThat(nombres(gestion.get("convocatorias").get(0))).containsExactlyInAnyOrder("codigo", "nombre");
    }
}
