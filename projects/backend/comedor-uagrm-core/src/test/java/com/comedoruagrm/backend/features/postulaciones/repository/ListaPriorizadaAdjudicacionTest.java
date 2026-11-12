package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * La lista priorizada (CU06) solo muestra postulaciones «calificadas»: una vez decidida por el CU05 (adjudicada o
 * no adjudicada) sale de la lista y no se puede volver a elegir ni decidir.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ListaPriorizadaRepository.class)
class ListaPriorizadaAdjudicacionTest {

    @Autowired
    private ListaPriorizadaRepository lista;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @Test
    void unaPostulacionAdjudicadaONoAdjudicada_noApareceEnLaListaPriorizada() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = fx.convocatoria("LPA1");
        String[] estados = {"calificada", "adjudicada", "no_adjudicada"};
        long calificada = 0;
        for (int i = 0; i < estados.length; i++) {
            long postulacion = fx.postulacion(conv, fx.postulante("953000" + i), estados[i]);
            fx.puntajeIa(postulacion, "5" + i, "{}");
            if (i == 0) {
                calificada = postulacion;
            }
        }
        UUID codigo = jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class,
                conv);
        UUID codigoCalificada = jdbc.queryForObject("SELECT codigo_postulacion FROM postulacion WHERE id = ?",
                UUID.class, calificada);

        var filas = lista.buscar(codigo, null, null, 50, 0);

        assertThat(filas).extracting(PostulacionPriorizadaResponse::codigo).containsExactly(codigoCalificada);
        assertThat(lista.contar(codigo, null, null)).isEqualTo(1);
    }
}
