package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios;
import com.comedoruagrm.backend.features.postulaciones.dto.EnviarPostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.GuardarBorradorRequest;

import tools.jackson.databind.json.JsonMapper;

class ComplementariosJsonTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void escribirYLeer_conservaTodosLosCampos() {
        DatosComplementarios datos = new DatosComplementarios("Ambos padres", "Rentista", "Menor de Bs. 2500", true,
                "Más de 1", List.of(new DatosComplementarios.MiembroGrupoFamiliar("María", "Madre", 45, "Ama de casa",
                        "Sin observaciones")),
                new DatosComplementarios.Residencia("Andrés Ibáñez", "3er anillo", "Plan 3000", "Calle 5"), "Alquiler",
                new DatosComplementarios.Infraestructura(2, 1, 1, 1, 0), true, "Beca de trabajo", false, null,
                "Comentario");

        assertThat(ComplementariosJson.leer(ComplementariosJson.escribir(datos))).isEqualTo(datos);
    }

    @Test
    void escribir_sinDatos_devuelveNulo() {
        assertThat(ComplementariosJson.escribir(null)).isNull();
    }

    @Test
    void leer_unJsonQueNoSePuedeInterpretar_noTumbaYDevuelveNulo() {
        assertThat(ComplementariosJson.leer("{esto no es json")).isNull();
        assertThat(ComplementariosJson.leer(" ")).isNull();
        assertThat(ComplementariosJson.leer(null)).isNull();
    }

    @Test
    void elCuerpoDelEnvio_conComplementarios_seDeserializaDelJsonDelCliente() {
        String json = """
                {"ingresosFamiliares": 1800.50, "dependientes": 4, "procedencia": "Provincia",
                 "patrimonio": "Vivienda propia", "estadoVulnerabilidad": "Hogar monoparental",
                 "promedioAcademico": 72.5,
                 "complementarios": {"dependenciaEconomica": "Independiente", "tieneHijos": false,
                   "infraestructura": {"dormitorios": 2}}}
                """;

        EnviarPostulacionRequest r = MAPPER.readValue(json, EnviarPostulacionRequest.class);

        assertThat(r.ingresosFamiliares()).isEqualByComparingTo(new BigDecimal("1800.50"));
        assertThat(r.complementarios().dependenciaEconomica()).isEqualTo("Independiente");
        assertThat(r.complementarios().tieneHijos()).isFalse();
        assertThat(r.complementarios().infraestructura().dormitorios()).isEqualTo(2);
    }

    @Test
    void elCuerpoDeUnClienteAntiguoSinComplementarios_sigueDeserializandose() {
        String json = """
                {"ingresosFamiliares": 1800, "dependientes": 4, "procedencia": "Provincia"}
                """;

        assertThat(MAPPER.readValue(json, EnviarPostulacionRequest.class).complementarios()).isNull();
        assertThat(MAPPER.readValue(json, GuardarBorradorRequest.class).procedencia()).isEqualTo("Provincia");
    }
}
