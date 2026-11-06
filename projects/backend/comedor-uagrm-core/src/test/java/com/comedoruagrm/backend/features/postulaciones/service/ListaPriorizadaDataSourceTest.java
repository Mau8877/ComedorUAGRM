package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.postulaciones.dto.CarreraPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.design.JRDesignField;

class ListaPriorizadaDataSourceTest {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");

    private PostulacionPriorizadaResponse fila(long posicion) {
        return new PostulacionPriorizadaResponse(UUID.randomUUID(), posicion, "Ana", "Rojas Vaca", "9802041",
                new CarreraPriorizadaResponse(UUID.randomUUID(), "Ing. Sistemas"),
                new BigDecimal("95.50"), Instant.parse("2026-09-01T10:00:00Z"), "calificada");
    }

    private JRField campo(String nombre) {
        JRDesignField campo = new JRDesignField();
        campo.setName(nombre);
        return campo;
    }

    @Test
    void next_pideLosTramosHastaQueUnoVieneVacioYRecorreTodasLasFilasEnOrden() throws JRException {
        List<Long> desplazamientos = new ArrayList<>();
        ListaPriorizadaDataSource datos = new ListaPriorizadaDataSource(desplazamiento -> {
            desplazamientos.add(desplazamiento);
            if (desplazamiento == 0) {
                return List.of(fila(1), fila(2));
            }
            return desplazamiento == 2 ? List.of(fila(3)) : List.of();
        }, LA_PAZ);

        List<String> posiciones = new ArrayList<>();
        while (datos.next()) {
            posiciones.add((String) datos.getFieldValue(campo("posicion")));
        }

        assertThat(posiciones).containsExactly("1", "2", "3");
        assertThat(desplazamientos).containsExactly(0L, 2L, 3L);
        assertThat(datos.leidas()).isEqualTo(3);
        // Terminado: next() ya no vuelve a pedir tramos.
        assertThat(datos.next()).isFalse();
        assertThat(desplazamientos).hasSize(3);
    }

    @Test
    void getFieldValue_formateaLosCamposComoTexto() throws JRException {
        ListaPriorizadaDataSource datos = new ListaPriorizadaDataSource(
                desplazamiento -> desplazamiento == 0 ? List.of(fila(7)) : List.of(), LA_PAZ);
        datos.next();

        assertThat(datos.getFieldValue(campo("posicion"))).isEqualTo("7");
        assertThat(datos.getFieldValue(campo("postulante"))).isEqualTo("Ana Rojas Vaca");
        assertThat(datos.getFieldValue(campo("ci"))).isEqualTo("9802041");
        assertThat(datos.getFieldValue(campo("carrera"))).isEqualTo("Ing. Sistemas");
        assertThat(datos.getFieldValue(campo("puntaje"))).isEqualTo("95.50");
        assertThat(datos.getFieldValue(campo("fechaCalculo"))).isEqualTo("01/09/2026");
    }

    @Test
    void getFieldValue_conUnCampoDesconocido_falla() {
        ListaPriorizadaDataSource datos = new ListaPriorizadaDataSource(
                desplazamiento -> desplazamiento == 0 ? List.of(fila(1)) : List.of(), LA_PAZ);
        datos.next();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> datos.getFieldValue(campo("otro")))
                .isInstanceOf(JRException.class);
    }
}
