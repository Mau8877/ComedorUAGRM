package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.FilaListado;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.JdbcContadorDeConsultas;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas de «Mis postulaciones» (CU04) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MisPostulacionesConsultaRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private ExpedienteFixtures fx;
    private JdbcContadorDeConsultas contador;
    private MisPostulacionesConsultaRepository consulta;
    private long yo;
    private long otro;
    private long admin;
    private int siguienteCi;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @BeforeEach
    void preparar() {
        fx = new ExpedienteFixtures(jdbc);
        contador = new JdbcContadorDeConsultas(dataSource);
        consulta = new MisPostulacionesConsultaRepository(new NamedParameterJdbcTemplate(contador));
        yo = fx.postulante("9810001");
        otro = fx.postulante("9810002");
        admin = fx.usuario("9810003", "ADMINISTRADOR_DUBSS");
        siguienteCi = 9820000;
    }

    /** Una postulacion del usuario en la convocatoria y estado dados, enviada en `envio` (ISO). */
    private long postulacion(long usuario, long convocatoria, String estado, String envio) {
        long id = fx.postulacion(convocatoria, usuario, estado);
        if (envio != null) {
            fx.fechaEnvio(id, envio);
        }
        return id;
    }

    private void publicar(long convocatoria) {
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por) VALUES (?, ?)", convocatoria,
                admin);
    }

    private UUID codigoGestionDe(long convocatoria) {
        return jdbc.queryForObject("SELECT g.codigo_gestion_academica FROM convocatoria cv JOIN gestion_academica g "
                + "ON g.id = cv.gestion_id WHERE cv.id = ?", UUID.class, convocatoria);
    }

    private static PageRequest pagina() {
        return PageRequest.of(0, 20);
    }

    @Test
    void listar_excluyeBorradoresYPostulacionesAjenas() {
        long c = fx.convocatoria("MP1");
        postulacion(yo, c, "pendiente", "2026-11-01T10:00:00Z");
        postulacion(yo, fx.convocatoria("MP1B"), "borrador", null);
        postulacion(otro, c, "pendiente", "2026-11-02T10:00:00Z");

        var resultado = consulta.listar(yo, null, false, pagina());

        assertThat(resultado.total()).isEqualTo(1);
        assertThat(resultado.filas()).hasSize(1);
        FilaListado fila = resultado.filas().get(0);
        assertThat(fila.estado()).isEqualTo("pendiente");
        assertThat(fila.convocatoriaNombre()).isEqualTo("Conv MP1");
        assertThat(fila.gestionNombre()).isEqualTo("G-MP1");
        assertThat(fila.numeroEnvios()).isEqualTo((short) 1);
        assertThat(fila.resultadosPublicados()).isFalse();
    }

    @Test
    void listar_ordenaPorFechaDeEnvioDeLaMasRecienteALaMasAntigua_yAlReves() {
        postulacion(yo, fx.convocatoria("MP2A"), "pendiente", "2026-11-01T10:00:00Z");
        postulacion(yo, fx.convocatoria("MP2B"), "calificada", "2026-11-03T10:00:00Z");
        postulacion(yo, fx.convocatoria("MP2C"), "devuelta", "2026-11-02T10:00:00Z");

        assertThat(consulta.listar(yo, null, false, pagina()).filas()).extracting(FilaListado::convocatoriaNombre)
                .containsExactly("Conv MP2B", "Conv MP2C", "Conv MP2A");
        assertThat(consulta.listar(yo, null, true, pagina()).filas()).extracting(FilaListado::convocatoriaNombre)
                .containsExactly("Conv MP2A", "Conv MP2C", "Conv MP2B");
    }

    @Test
    void listar_paginaConElTotal() {
        for (int i = 0; i < 5; i++) {
            postulacion(yo, fx.convocatoria("MP3" + i), "pendiente", "2026-11-0" + (i + 1) + "T10:00:00Z");
        }

        var segunda = consulta.listar(yo, null, false, PageRequest.of(1, 2));

        assertThat(segunda.total()).isEqualTo(5);
        assertThat(segunda.filas()).extracting(FilaListado::convocatoriaNombre).containsExactly("Conv MP32", "Conv MP31");
    }

    @Test
    void listar_filtraPorGestionAcademica() {
        long a = fx.convocatoria("MP4A");
        long b = fx.convocatoria("MP4B");
        postulacion(yo, a, "pendiente", "2026-11-01T10:00:00Z");
        postulacion(yo, b, "pendiente", "2026-11-02T10:00:00Z");

        var soloA = consulta.listar(yo, codigoGestionDe(a), false, pagina());

        assertThat(soloA.total()).isEqualTo(1);
        assertThat(soloA.filas()).extracting(FilaListado::convocatoriaNombre).containsExactly("Conv MP4A");
        assertThat(consulta.listar(yo, UUID.randomUUID(), false, pagina()).filas()).isEmpty();
    }

    @Test
    void listar_marcaSiLosResultadosDeLaConvocatoriaEstanPublicados_sinMultiplicarFilas() {
        long publicada = fx.convocatoria("MP5A");
        long sinPublicar = fx.convocatoria("MP5B");
        postulacion(yo, publicada, "adjudicada", "2026-11-01T10:00:00Z");
        postulacion(yo, sinPublicar, "adjudicada", "2026-11-02T10:00:00Z");
        publicar(publicada);
        // Una publicacion eliminada no cuenta.
        jdbc.update("INSERT INTO publicacion_resultado (convocatoria_id, publicado_por, eliminado_en) "
                + "VALUES (?, ?, now())", sinPublicar, admin);

        var resultado = consulta.listar(yo, null, true, pagina());

        assertThat(resultado.filas()).extracting(FilaListado::resultadosPublicados).containsExactly(true, false);
        assertThat(resultado.total()).isEqualTo(2);
    }

    @Test
    void listar_noCuentaLoEliminado() {
        long c = fx.convocatoria("MP6");
        long p = postulacion(yo, c, "pendiente", "2026-11-01T10:00:00Z");
        long eliminadaConv = fx.convocatoria("MP6B");
        postulacion(yo, eliminadaConv, "pendiente", "2026-11-02T10:00:00Z");
        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE id = ?", eliminadaConv);
        jdbc.update("UPDATE postulacion SET eliminado_en = now() WHERE id = ?", p);

        assertThat(consulta.listar(yo, null, false, pagina()).total()).isZero();
    }

    @Test
    void listar_sinPostulaciones_devuelveVacio() {
        var resultado = consulta.listar(yo, null, false, pagina());

        assertThat(resultado.filas()).isEqualTo(List.of());
        assertThat(resultado.total()).isZero();
    }

    @Test
    void listar_haceDosConsultasSinImportarCuantasPostulacionesHaya() {
        for (int i = 0; i < 3; i++) {
            postulacion(yo, fx.convocatoria("MP7" + i), "pendiente", "2026-11-0" + (i + 1) + "T10:00:00Z");
        }
        contador.reiniciar();
        consulta.listar(yo, null, false, pagina());
        int con3 = contador.consultas();
        for (int i = 3; i < 9; i++) {
            long c = fx.convocatoria("MP7" + i);
            postulacion(yo, c, "adjudicada", "2026-11-0" + (i + 1) + "T10:00:00Z");
            publicar(c);
        }

        contador.reiniciar();
        consulta.listar(yo, null, false, pagina());

        assertThat(con3).isEqualTo(2);
        assertThat(contador.consultas()).isEqualTo(con3);
    }
}
