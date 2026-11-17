package com.comedoruagrm.backend.features.preseleccion.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Las consultas del calculo y del recalculo del puntaje (CU03 y CU26) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PrecalificacionConsultaRepository.class)
class PrecalificacionConsultaRepositoryTest {

    @Autowired
    private PrecalificacionConsultaRepository consulta;

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

    private long postulacion(String etiqueta, String ci, String estado) {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long postulacion = fx.postulacion(fx.convocatoria(etiqueta), fx.postulante(ci), estado);
        fx.respuestaSocioeconomica(postulacion, "70.00");
        return postulacion;
    }

    @Test
    void datosPendienteYDatosCalificada_cadaUnaVeSoloSuEstado() {
        long pendiente = postulacion("PC1", "9620001", "pendiente");
        long calificada = postulacion("PC2", "9620002", "calificada");

        assertThat(consulta.datosPendiente(pendiente)).isPresent();
        assertThat(consulta.datosCalificada(pendiente)).isEmpty();
        assertThat(consulta.datosCalificada(calificada)).isPresent();
        assertThat(consulta.datosPendiente(calificada)).isEmpty();
        var datos = consulta.datosCalificada(calificada).orElseThrow();
        assertThat(datos.dependientes()).isEqualTo(4);
        assertThat(datos.carrera()).isNotBlank();
    }

    @Test
    void datos_leeLosDatosDelFormularioOficialDeDatosExtra() {
        long pendiente = postulacion("PC6", "9620006", "pendiente");
        jdbc.update("""
                UPDATE respuesta_socioeconomica SET datos_extra = CAST(? AS jsonb) WHERE postulacion_id = ?""",
                """
                {"dependenciaEconomica":"Independiente","ocupacionSosten":"Asalariado informal",
                 "tieneHijos":true,"cantidadHijos":"Más de 1","discapacidad":false,"comentario":"no se manda"}
                """, pendiente);

        var datos = consulta.datosPendiente(pendiente).orElseThrow();

        assertThat(datos.dependenciaEconomica()).isEqualTo("Independiente");
        assertThat(datos.ocupacionSosten()).isEqualTo("Asalariado informal");
        assertThat(datos.tieneHijos()).isTrue();
        assertThat(datos.cantidadHijos()).isEqualTo("Más de 1");
        assertThat(datos.discapacidad()).isFalse();
    }

    @Test
    void datos_sinDatosExtraOConValoresNulosDevuelveNulos() {
        long sinDatos = postulacion("PC7", "9620007", "pendiente");
        long conNulos = postulacion("PC8", "9620008", "pendiente");
        jdbc.update("""
                UPDATE respuesta_socioeconomica SET datos_extra = CAST(? AS jsonb) WHERE postulacion_id = ?""",
                "{\"dependenciaEconomica\":null,\"tieneHijos\":null,\"discapacidad\":null}", conNulos);

        for (long id : new long[] {sinDatos, conNulos}) {
            var datos = consulta.datosPendiente(id).orElseThrow();
            assertThat(datos.dependenciaEconomica()).isNull();
            assertThat(datos.ocupacionSosten()).isNull();
            assertThat(datos.tieneHijos()).isNull();
            assertThat(datos.cantidadHijos()).isNull();
            assertThat(datos.discapacidad()).isNull();
        }
    }

    @Test
    void sigueCalificadaYSigueEnPendiente_siguenElEstadoActual() {
        long calificada = postulacion("PC3", "9620003", "calificada");

        assertThat(consulta.sigueCalificada(calificada)).isTrue();
        assertThat(consulta.sigueEnPendiente(calificada)).isFalse();
    }

    @Test
    void tieneVigenteDelModelo_soloSiElPuntajeVigenteEsDeEsaVersion() {
        long calificada = postulacion("PC4", "9620004", "calificada");
        long modelo = jdbc.queryForObject("""
                INSERT INTO modelo_preseleccion (version, activo) VALUES ('v9.20', false) RETURNING id""", Long.class);
        long otro = jdbc.queryForObject("""
                INSERT INTO modelo_preseleccion (version, activo) VALUES ('v9.21', false) RETURNING id""", Long.class);
        jdbc.update("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                VALUES (?, ?, 40, now(), true)""", modelo, calificada);

        assertThat(consulta.tieneVigenteDelModelo(calificada, modelo)).isTrue();
        assertThat(consulta.tieneVigenteDelModelo(calificada, otro)).isFalse();
    }

    @Test
    void retirarVigentes_pasaElPuntajeAHistorialYPermiteInsertarElNuevo() {
        long calificada = postulacion("PC5", "9620005", "calificada");
        long modelo = jdbc.queryForObject("""
                INSERT INTO modelo_preseleccion (version, activo) VALUES ('v9.22', false) RETURNING id""", Long.class);
        jdbc.update("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                VALUES (?, ?, 40, now(), true)""", modelo, calificada);

        assertThat(consulta.retirarVigentes(calificada)).isEqualTo(1);
        jdbc.update("""
                INSERT INTO puntaje_postulacion (modelo_id, postulacion_id, puntaje_total, fecha_calculo, vigente)
                VALUES (?, ?, 55, now(), true)""", modelo, calificada);

        Integer total = jdbc.queryForObject("SELECT count(*) FROM puntaje_postulacion WHERE postulacion_id = ?",
                Integer.class, calificada);
        Integer vigentes = jdbc.queryForObject(
                "SELECT count(*) FROM puntaje_postulacion WHERE postulacion_id = ? AND vigente", Integer.class,
                calificada);
        assertThat(total).isEqualTo(2);
        assertThat(vigentes).isEqualTo(1);
    }

    @Test
    void registrarFallo_alLlegarAlMaximoMarcaParaRevisionYElReintentoYaNoLaToma() {
        long id = postulacion("PC6", "9620006", "pendiente");

        assertThat(consulta.pendientes(100)).contains(id);
        assertThat(consulta.registrarFallo(id, 3)).isFalse();
        assertThat(consulta.registrarFallo(id, 3)).isFalse();
        assertThat(consulta.pendientes(100)).contains(id);
        assertThat(consulta.registrarFallo(id, 3)).isTrue();

        assertThat(consulta.pendientes(100)).doesNotContain(id);
        assertThat(jdbc.queryForObject("SELECT intentos_precalificacion FROM postulacion WHERE id = ?",
                Integer.class, id)).isEqualTo(3);
        // sigue `pendiente`: no es un estado nuevo
        assertThat(consulta.sigueEnPendiente(id)).isTrue();
    }

    @Test
    void registrarFallo_soloCuentaSobreUnaPostulacionPendiente() {
        long calificada = postulacion("PC7", "9620007", "calificada");

        assertThat(consulta.registrarFallo(calificada, 1)).isFalse();
        assertThat(jdbc.queryForObject("SELECT intentos_precalificacion FROM postulacion WHERE id = ?",
                Integer.class, calificada)).isZero();
    }

    @Test
    void cabeceraPosicionEHistorial_leenElPuntajeVigenteElLugarEnLaListaYLosCambiosDeEstado() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long convocatoria = fx.convocatoria("PC8");
        long primero = fx.postulacion(convocatoria, fx.postulante("9620008"), "calificada");
        long segundo = fx.postulacion(convocatoria, fx.postulante("9620009"), "calificada");
        long pendiente = fx.postulacion(convocatoria, fx.postulante("9620010"), "pendiente");
        fx.puntajeIa(primero, "60.00", "{\"variables\": {}}");
        fx.puntajeIa(segundo, "50.00", "{\"variables\": {}}");
        jdbc.update("""
                INSERT INTO postulacion_historial (postulacion_id, estado_anterior, estado_nuevo, motivo)
                VALUES (?, 'pendiente', 'calificada', 'Precalificacion automatica')""", segundo);

        var cabecera = consulta.cabecera(java.util.UUID.fromString(jdbc.queryForObject(
                "SELECT codigo_postulacion::text FROM postulacion WHERE id = ?", String.class, segundo))).orElseThrow();
        assertThat(cabecera.id()).isEqualTo(segundo);
        assertThat(cabecera.puntaje()).isEqualByComparingTo("50.00");
        assertThat(cabecera.estado()).isEqualTo("calificada");
        assertThat(cabecera.convocatoria()).isNotBlank();
        assertThat(cabecera.detalle()).contains("variables");

        assertThat(consulta.posicion(primero)).contains(1L);
        assertThat(consulta.posicion(segundo)).contains(2L);
        assertThat(consulta.posicion(pendiente)).isEmpty();

        assertThat(consulta.historial(segundo)).extracting("estadoNuevo").containsExactly("calificada");
        assertThat(consulta.historial(segundo).get(0).motivo()).isEqualTo("Precalificacion automatica");

        var sinPuntaje = consulta.cabecera(java.util.UUID.fromString(jdbc.queryForObject(
                "SELECT codigo_postulacion::text FROM postulacion WHERE id = ?", String.class, pendiente)))
                .orElseThrow();
        assertThat(sinPuntaje.puntaje()).isNull();
        assertThat(sinPuntaje.modelo()).isNull();
        assertThat(consulta.cabecera(java.util.UUID.randomUUID())).isEmpty();
    }
}
