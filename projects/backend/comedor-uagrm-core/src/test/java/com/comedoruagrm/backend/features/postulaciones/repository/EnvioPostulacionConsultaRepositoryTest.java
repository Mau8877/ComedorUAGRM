package com.comedoruagrm.backend.features.postulaciones.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.Respuesta;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas y escrituras del envio de la postulacion (CU02) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EnvioPostulacionConsultaRepository.class)
class EnvioPostulacionConsultaRepositoryTest {

    @Autowired
    private EnvioPostulacionConsultaRepository consulta;

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

    private UUID codigoUsuario(long usuario) {
        return jdbc.queryForObject("SELECT codigo_usuario FROM usuario WHERE id = ?", UUID.class, usuario);
    }

    /** Una convocatoria `ABIERTA` en una gestion nueva, con las fechas dadas (en dias respecto de ahora). */
    private long convocatoria(String etiqueta, String estado, int abreEnDias, int cierraEnDias) {
        long gestion = jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES (?, current_date - 100, current_date + 100) RETURNING id
                """, Long.class, etiqueta);
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, nombre, fecha_apertura, fecha_cierre, estado)
                VALUES (?, ?, now() + (? * interval '1 day'), now() + (? * interval '1 day'),
                        CAST(? AS estado_convocatoria)) RETURNING id
                """, Long.class, gestion, "Convocatoria " + etiqueta, abreEnDias, cierraEnDias, estado);
    }

    @Test
    void perfil_devuelveLosDatosDelPostulanteConSuCarrera() {
        long usuario = new ExpedienteFixtures(jdbc).postulante("9500001");

        var perfil = consulta.perfil(codigoUsuario(usuario)).orElseThrow();

        assertThat(perfil.postulanteId()).isEqualTo(usuario);
        assertThat(perfil.ci()).isEqualTo("9500001");
        assertThat(perfil.carrera()).isNotBlank();
    }

    @Test
    void perfil_conCuentaQueNoEsDePostulante_devuelveVacio() {
        long admin = new ExpedienteFixtures(jdbc).usuario("9500002", "ADMINISTRADOR_DUBSS");

        assertThat(consulta.perfil(codigoUsuario(admin))).isEmpty();
    }

    @Test
    void convocatoriaAbierta_devuelveLaVigenteYNoLaCerradaNiLaQueTodaviaNoAbre() {
        long vigente = convocatoria("ENV1", "ABIERTA", -1, 5);
        convocatoria("ENV1-CERRADA", "CERRADA", -10, -5);
        convocatoria("ENV1-FUTURA", "ABIERTA", 5, 10);

        var abierta = consulta.convocatoriaAbierta(Instant.now()).orElseThrow();

        assertThat(abierta.id()).isEqualTo(vigente);
        assertThat(abierta.gestion()).isEqualTo("ENV1");
    }

    @Test
    void proximaConvocatoria_devuelveLaAbiertaQueTodaviaNoLlegoASuApertura() {
        convocatoria("ENV2", "ABIERTA", 3, 9);

        var proxima = consulta.proximaConvocatoria(Instant.now()).orElseThrow();

        assertThat(proxima.nombre()).isEqualTo("Convocatoria ENV2");
        assertThat(proxima.fechaApertura()).isAfter(Instant.now());
    }

    @Test
    void crearBorrador_dejaLaFilaEnBorradorYUnaSegundaVioleElIndiceUnico() {
        long usuario = new ExpedienteFixtures(jdbc).postulante("9500003");
        long conv = convocatoria("ENV3", "ABIERTA", -1, 5);

        var creada = consulta.crearBorrador(usuario, conv);

        assertThat(creada.estado()).isEqualTo("borrador");
        assertThat(creada.numeroEnvios()).isZero();
        assertThat(creada.fechaEnvio()).isNull();
        assertThat(consulta.postulacionDe(usuario, conv)).map(p -> p.id()).contains(creada.id());
        assertThatThrownBy(() -> consulta.crearBorrador(usuario, conv))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cabecera_devuelveLaPostulacionDelUsuarioYNoLaDeOtro() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long usuario = fx.postulante("9500004");
        long otro = fx.postulante("9500005");
        long conv = convocatoria("ENV4", "ABIERTA", -1, 5);
        var creada = consulta.crearBorrador(usuario, conv);

        var cabecera = consulta.cabecera(creada.codigo(), codigoUsuario(usuario)).orElseThrow();

        assertThat(cabecera.postulacionId()).isEqualTo(creada.id());
        assertThat(cabecera.convocatoriaId()).isEqualTo(conv);
        assertThat(cabecera.gestion()).isEqualTo("ENV4");
        assertThat(consulta.cabecera(creada.codigo(), codigoUsuario(otro))).isEmpty();
        assertThat(consulta.estadoActual(creada.id())).contains("borrador");
    }

    @Test
    void guardarRespuesta_creaLaFilaYUnaSegundaLlamadaLaReemplaza() {
        long usuario = new ExpedienteFixtures(jdbc).postulante("9500006");
        long conv = convocatoria("ENV5", "ABIERTA", -1, 5);
        long postulacion = consulta.crearBorrador(usuario, conv).id();
        assertThat(consulta.respuesta(postulacion)).isEmpty();

        consulta.guardarRespuesta(postulacion, new Respuesta(new BigDecimal("1800.00"), 4, "Provincia",
                "Sin vivienda propia", "Hogar monoparental", new BigDecimal("72.50")));
        consulta.guardarRespuesta(postulacion, new Respuesta(new BigDecimal("2500.50"), 2, "Capital",
                "Vivienda propia", "Sin situación especial", new BigDecimal("80")));

        var r = consulta.respuesta(postulacion).orElseThrow();
        assertThat(r.ingresosFamiliares()).isEqualByComparingTo("2500.50");
        assertThat(r.dependientes()).isEqualTo(2);
        assertThat(r.procedencia()).isEqualTo("Capital");
        assertThat(r.promedioAcademico()).isEqualByComparingTo("80");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM respuesta_socioeconomica WHERE postulacion_id = ?",
                Integer.class, postulacion)).isEqualTo(1);
    }

    @Test
    void guardarRespuesta_conUnPromedioFueraDeRango_violaElCheckDeLaTabla() {
        long usuario = new ExpedienteFixtures(jdbc).postulante("9500007");
        long conv = convocatoria("ENV6", "ABIERTA", -1, 5);
        long postulacion = consulta.crearBorrador(usuario, conv).id();

        assertThatThrownBy(() -> consulta.guardarRespuesta(postulacion, new Respuesta(new BigDecimal("1800"), 4,
                "Provincia", "Sin vivienda propia", "Hogar monoparental", new BigDecimal("150"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardarRespuesta_aceptaUnBorradorParcialConCamposNulos() {
        long usuario = new ExpedienteFixtures(jdbc).postulante("9500008");
        long conv = convocatoria("ENV7", "ABIERTA", -1, 5);
        long postulacion = consulta.crearBorrador(usuario, conv).id();

        consulta.guardarRespuesta(postulacion, new Respuesta(null, null, "Provincia", null, null, null));

        var r = consulta.respuesta(postulacion).orElseThrow();
        assertThat(r.procedencia()).isEqualTo("Provincia");
        assertThat(r.dependientes()).isNull();
        assertThat(r.ingresosFamiliares()).isNull();
    }

    @Test
    void complementarios_sinRespuestaOSinDatosEsVacio_yGuardarlosLosDevuelveTalCual() {
        long usuario = new ExpedienteFixtures(jdbc).postulante("9500031");
        long conv = convocatoria("ENV31", "ABIERTA", -1, 5);
        long postulacion = consulta.crearBorrador(usuario, conv).id();
        assertThat(consulta.complementarios(postulacion)).isEmpty();

        consulta.guardarRespuesta(postulacion, new Respuesta(null, 3, null, null, null, null));
        assertThat(consulta.complementarios(postulacion)).isEmpty();

        consulta.guardarComplementarios(postulacion,
                "{\"dependenciaEconomica\":\"Independiente\",\"tieneHijos\":true,\"cantidadHijos\":\"1\"}");

        String json = consulta.complementarios(postulacion).orElseThrow();
        assertThat(json).contains("Independiente").contains("\"tieneHijos\": true");
        assertThat(jdbc.queryForObject(
                "SELECT datos_extra ->> 'dependenciaEconomica' FROM respuesta_socioeconomica WHERE postulacion_id = ?",
                String.class, postulacion)).isEqualTo("Independiente");
    }

    @Test
    void guardarComplementarios_conNuloVaciaLosDatos_yGuardarLaRespuestaNoLosPisa() {
        long usuario = new ExpedienteFixtures(jdbc).postulante("9500032");
        long conv = convocatoria("ENV32", "ABIERTA", -1, 5);
        long postulacion = consulta.crearBorrador(usuario, conv).id();
        consulta.guardarRespuesta(postulacion, new Respuesta(null, 3, null, null, null, null));
        consulta.guardarComplementarios(postulacion, "{\"discapacidad\":true}");

        // * Reenviar las columnas (UPSERT) no toca `datos_extra`.
        consulta.guardarRespuesta(postulacion, new Respuesta(null, 5, null, null, null, null));
        assertThat(consulta.complementarios(postulacion)).isPresent();

        consulta.guardarComplementarios(postulacion, null);

        assertThat(consulta.complementarios(postulacion)).isEmpty();
    }

    @Test
    void guardadoEn_sinRespuestaEsVacioYConRespuestaEsLaFechaDelUltimoGuardado() {
        long usuario = new ExpedienteFixtures(jdbc).postulante("9500009");
        long conv = convocatoria("ENV8", "ABIERTA", -1, 5);
        long postulacion = consulta.crearBorrador(usuario, conv).id();
        assertThat(consulta.guardadoEn(postulacion)).isEmpty();

        consulta.guardarRespuesta(postulacion, new Respuesta(null, 3, null, null, null, null));

        assertThat(consulta.guardadoEn(postulacion)).isPresent();
    }

    @Test
    void borradorAbierto_soloDevuelveElBorradorDeUnaConvocatoriaAbiertaYVigente() {
        var fixtures = new ExpedienteFixtures(jdbc);
        long usuario = fixtures.postulante("9500010");
        long abierta = convocatoria("ENV9", "ABIERTA", -1, 5);
        long cerrada = convocatoria("ENV10", "CERRADA", -10, -1);
        var enAbierta = consulta.crearBorrador(usuario, abierta);
        long otro = fixtures.postulante("9500011");
        consulta.crearBorrador(otro, cerrada);

        var propio = consulta.borradorAbierto(codigoUsuario(usuario), java.time.Instant.now()).orElseThrow();

        assertThat(propio.postulacionId()).isEqualTo(enAbierta.id());
        assertThat(propio.codigo()).isEqualTo(enAbierta.codigo());
        assertThat(propio.gestion()).isEqualTo("ENV9");
        assertThat(consulta.borradorAbierto(codigoUsuario(otro), java.time.Instant.now())).isEmpty();
    }
}
