package com.comedoruagrm.backend.features.notificaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
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

import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionRepository.CodigoDePostulacion;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Las consultas propias del codigo de postulacion de un aviso (CU04) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(NotificacionLoteRepository.class)
class NotificacionPostulacionCodigoRepositoryTest {

    @Autowired
    private NotificacionRepository repositorio;

    @Autowired
    private NotificacionLoteRepository loteRepositorio;

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

    private long postulacion(String ci, String etiqueta) {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        return fx.postulacion(fx.convocatoria(etiqueta), fx.postulante(ci), "pendiente");
    }

    private UUID codigo(long postulacion) {
        return jdbc.queryForObject("SELECT codigo_postulacion FROM postulacion WHERE id = ?", UUID.class, postulacion);
    }

    @Test
    void codigoDePostulacion_devuelveElCodigoPublico_yVacioSiNoExisteOEstaEliminada() {
        long p = postulacion("9840001", "NPC1");
        long eliminada = postulacion("9840002", "NPC2");
        jdbc.update("UPDATE postulacion SET eliminado_en = now() WHERE id = ?", eliminada);

        assertThat(repositorio.codigoDePostulacion(p)).contains(codigo(p));
        assertThat(repositorio.codigoDePostulacion(eliminada)).isEmpty();
        assertThat(repositorio.codigoDePostulacion(-1L)).isEmpty();
    }

    @Test
    void codigosDePostulaciones_resuelveVariasEnUnaConsulta_ySaltaLasInexistentes() {
        long a = postulacion("9840003", "NPC3");
        long b = postulacion("9840004", "NPC4");

        List<CodigoDePostulacion> filas = repositorio.codigosDePostulaciones(List.of(a, b, -1L));

        assertThat(filas).hasSize(2);
        assertThat(filas).extracting(CodigoDePostulacion::getId).containsExactlyInAnyOrder(a, b);
        assertThat(filas.stream().filter(f -> f.getId().equals(a)).findFirst().orElseThrow().getCodigo())
                .isEqualTo(codigo(a));
    }

    @Test
    void loteRepositorio_resuelveLosCodigosPorId_yConUnaListaVaciaNoConsulta() {
        long a = postulacion("9840005", "NPC5");
        long b = postulacion("9840006", "NPC6");

        Map<Long, UUID> codigos = loteRepositorio.codigosDePostulaciones(List.of(a, b));

        assertThat(codigos).containsEntry(a, codigo(a)).containsEntry(b, codigo(b));
        assertThat(loteRepositorio.codigosDePostulaciones(List.of())).isEmpty();
    }
}
