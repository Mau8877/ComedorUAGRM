package com.comedoruagrm.backend.features.finalistas.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

import com.comedoruagrm.backend.features.finalistas.dto.FinalistaResponse;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Consulta de finalistas (CU21): estado de la notificacion derivado del ultimo aviso, busqueda y
 * paginacion, con Postgres real. Cada test se revierte.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FinalistaConsultaRepository.class)
class FinalistaConsultaRepositoryTest {

    @Autowired
    private FinalistaConsultaRepository consulta;

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

    private long convocatoria() {
        long gestion = jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES ('G-CONSULTA', DATE '2026-07-01', DATE '2026-12-31') RETURNING id""", Long.class);
        return jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, nombre, fecha_apertura, fecha_cierre, estado)
                VALUES (?, 'Conv', '2026-07-01T00:00:00Z', '2026-08-31T00:00:00Z',
                        CAST('CERRADA' AS estado_convocatoria)) RETURNING id""", Long.class, gestion);
    }

    private UUID codigoConvocatoria(long convocatoria) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class,
                convocatoria);
    }

    /** Crea un postulante con su postulacion en la convocatoria y lo deja como finalista en esa posicion. */
    private long finalista(long convocatoria, int posicion, String ci, String nombres, String apellidos) {
        long usuario = jdbc.queryForObject("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, '!', ?, ? FROM rol WHERE nombre = 'POSTULANTE' RETURNING id""",
                Long.class, ci, ci + "@prueba.test", nombres, apellidos);
        jdbc.update("""
                INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono)
                VALUES (?, (SELECT min(id) FROM carrera), ?, '70000000')""", usuario, "RU-" + ci);
        long postulacion = jdbc.queryForObject("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, numero_envios)
                VALUES (?, ?, CAST('calificada' AS estado_postulacion), 1) RETURNING id""", Long.class,
                convocatoria, usuario);
        jdbc.update("INSERT INTO finalista (postulacion_id, posicion) VALUES (?, ?)", postulacion, posicion);
        return postulacion;
    }

    private void aviso(long postulacion, String enviadaEn, String creadoEn, Long comunicado) {
        jdbc.update("""
                INSERT INTO notificacion (usuario_id, tipo, titulo, mensaje, postulacion_id, comunicado_id,
                                          enviada_en, creado_en)
                SELECT postulante_id, CAST('INFO' AS tipo_notificacion), 'T', 'M', id, ?,
                       CAST(? AS timestamptz), CAST(? AS timestamptz)
                FROM postulacion WHERE id = ?""", comunicado, enviadaEn, creadoEn, postulacion);
    }

    private String estado(UUID convocatoria, int posicion) {
        return consulta.buscar(convocatoria, null, 50, 0).stream()
                .filter(f -> f.posicion() == posicion).findFirst().orElseThrow().estadoNotificacion();
    }

    @Test
    void estado_sinAvisoEsSinEnviar_conEnviadaEnEsEnviada_yConEnviadaEnNuloEsFallida() {
        long conv = convocatoria();
        finalista(conv, 1, "9100001", "Ana", "Uno");
        long enviada = finalista(conv, 2, "9100002", "Beto", "Dos");
        long fallida = finalista(conv, 3, "9100003", "Carla", "Tres");
        aviso(enviada, "2026-11-06T10:00:01Z", "2026-11-06T10:00:00Z", null);
        aviso(fallida, null, "2026-11-06T10:00:00Z", null);

        UUID codigo = codigoConvocatoria(conv);
        assertThat(estado(codigo, 1)).isEqualTo("SIN_ENVIAR");
        assertThat(estado(codigo, 2)).isEqualTo("ENVIADA");
        assertThat(estado(codigo, 3)).isEqualTo("FALLIDA");
    }

    @Test
    void estado_soloCuentaElUltimoAviso() {
        long conv = convocatoria();
        long postulacion = finalista(conv, 1, "9100011", "Ana", "Uno");
        aviso(postulacion, null, "2026-11-06T10:00:00Z", null);
        aviso(postulacion, "2026-11-06T12:00:01Z", "2026-11-06T12:00:00Z", null);

        assertThat(estado(codigoConvocatoria(conv), 1)).isEqualTo("ENVIADA");
    }

    @Test
    void estado_elAvisoDeUnComunicadoNoCuenta() {
        long conv = convocatoria();
        long postulacion = finalista(conv, 1, "9100021", "Ana", "Uno");
        long admin = jdbc.queryForObject("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, '9100099', 'admin99@prueba.test', '!', 'Admin', 'DUBSS' FROM rol
                WHERE nombre = 'ADMINISTRADOR_DUBSS' RETURNING id""", Long.class);
        long comunicado = jdbc.queryForObject("""
                INSERT INTO comunicado (publicado_por, titulo, contenido, destinatarios)
                VALUES (?, 'T', 'C', CAST('TODOS' AS destinatarios_comunicado)) RETURNING id""", Long.class, admin);
        aviso(postulacion, "2026-11-06T10:00:01Z", "2026-11-06T10:00:00Z", comunicado);

        assertThat(estado(codigoConvocatoria(conv), 1)).isEqualTo("SIN_ENVIAR");
    }

    @Test
    void busqueda_porNombreOCi_pagina_yEscapaLosComodines() {
        long conv = convocatoria();
        finalista(conv, 1, "9100031", "Ana", "Rojas");
        finalista(conv, 2, "9100032", "Beto", "Soliz");
        finalista(conv, 3, "9100033", "Carla", "Rojas");
        UUID codigo = codigoConvocatoria(conv);

        assertThat(consulta.contar(codigo, null)).isEqualTo(3);
        List<FinalistaResponse> porApellido = consulta.buscar(codigo, "%rojas%", 50, 0);
        assertThat(porApellido).extracting(FinalistaResponse::posicion).containsExactly(1, 3);
        assertThat(consulta.contar(codigo, "%9100032%")).isEqualTo(1);
        // Un `%` literal escapado no es comodin: no encuentra a nadie.
        assertThat(consulta.contar(codigo, "%\\%%")).isZero();

        List<FinalistaResponse> segunda = consulta.buscar(codigo, null, 1, 1);
        assertThat(segunda).extracting(FinalistaResponse::posicion).containsExactly(2);
    }

    // ---- Auditoria del expediente (CU07) ----

    private FinalistaResponse fila(UUID convocatoria, int posicion) {
        return consulta.buscar(convocatoria, null, 50, 0).stream()
                .filter(f -> f.posicion() == posicion).findFirst().orElseThrow();
    }

    private long idFinalista(long postulacion) {
        return jdbc.queryForObject("SELECT id FROM finalista WHERE postulacion_id = ?", Long.class, postulacion);
    }

    @Test
    void auditoria_sinAuditoriaEsSinAuditar_yElTotalEsNulo() {
        long conv = convocatoria();
        long postulacion = finalista(conv, 1, "9100041", "Ana", "Uno");
        new ExpedienteFixtures(jdbc).puntajeIa(postulacion, "55.00", "{}");

        FinalistaResponse f = fila(codigoConvocatoria(conv), 1);

        assertThat(f.resultadoAuditoria()).isEqualTo("SIN_AUDITAR");
        assertThat(f.puntaje()).isEqualByComparingTo("55.00");
        assertThat(f.puntajeTotal()).isNull();
    }

    @Test
    void auditoria_tomaLaUltimaYSumaIaMasAcademico() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = convocatoria();
        long postulacion = finalista(conv, 1, "9100051", "Ana", "Uno");
        fx.puntajeIa(postulacion, "55.00", "{}");
        long finalista = idFinalista(postulacion);
        long auditor = fx.usuario("9100059", "ADMINISTRADOR_DUBSS");
        fx.auditoria(finalista, auditor, "VALIDADO", null, "10.00", "2026-11-05T10:00:00Z");
        fx.auditoria(finalista, auditor, "VALIDADO", null, "25.50", "2026-11-06T10:00:00Z");

        FinalistaResponse f = fila(codigoConvocatoria(conv), 1);

        assertThat(f.resultadoAuditoria()).isEqualTo("VALIDADO");
        assertThat(f.puntajeTotal()).isEqualByComparingTo("80.50");
    }

    @Test
    void auditoria_ultimaObservadaSinPuntaje_tieneResultadoPeroNoTotal() {
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        long conv = convocatoria();
        long postulacion = finalista(conv, 1, "9100061", "Ana", "Uno");
        fx.puntajeIa(postulacion, "55.00", "{}");
        long auditor = fx.usuario("9100069", "ADMINISTRADOR_DUBSS");
        fx.auditoria(idFinalista(postulacion), auditor, "OBSERVADO", "Falta carnet", null, "2026-11-06T10:00:00Z");

        FinalistaResponse f = fila(codigoConvocatoria(conv), 1);

        assertThat(f.resultadoAuditoria()).isEqualTo("OBSERVADO");
        assertThat(f.puntajeTotal()).isNull();
    }

    @Test
    void estadoPostulacion_muestraCalificadaYDevuelta() {
        long conv = convocatoria();
        finalista(conv, 1, "9100071", "Ana", "Uno");
        long devuelta = finalista(conv, 2, "9100072", "Beto", "Dos");
        jdbc.update("UPDATE postulacion SET estado = CAST('devuelta' AS estado_postulacion) WHERE id = ?",
                devuelta);

        assertThat(fila(codigoConvocatoria(conv), 1).estadoPostulacion()).isEqualTo("calificada");
        assertThat(fila(codigoConvocatoria(conv), 2).estadoPostulacion()).isEqualTo("devuelta");
    }

    @Test
    void estadoPostulacion_muestraArchivadaYNoAdjudicada_yElFinalistaSigueEnLaLista() {
        long conv = convocatoria();
        finalista(conv, 1, "9100081", "Ana", "Uno");
        long archivada = finalista(conv, 2, "9100082", "Beto", "Dos");
        long noAdjudicada = finalista(conv, 3, "9100083", "Carla", "Tres");
        jdbc.update("UPDATE postulacion SET estado = CAST('archivada' AS estado_postulacion) WHERE id = ?",
                archivada);
        jdbc.update("UPDATE postulacion SET estado = CAST('no_adjudicada' AS estado_postulacion) WHERE id = ?",
                noAdjudicada);

        assertThat(fila(codigoConvocatoria(conv), 1).estadoPostulacion()).isEqualTo("calificada");
        assertThat(fila(codigoConvocatoria(conv), 2).estadoPostulacion()).isEqualTo("archivada");
        assertThat(fila(codigoConvocatoria(conv), 3).estadoPostulacion()).isEqualTo("no_adjudicada");
    }
}
