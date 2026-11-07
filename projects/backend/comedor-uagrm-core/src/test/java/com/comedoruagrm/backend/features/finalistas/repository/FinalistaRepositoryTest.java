package com.comedoruagrm.backend.features.finalistas.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;
import com.comedoruagrm.backend.features.comunicados.model.Comunicado;
import com.comedoruagrm.backend.features.comunicados.repository.ComunicadoRepository;
import com.comedoruagrm.backend.features.finalistas.model.Finalista;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Finalistas y comunicados (CU21): guardar, leer, unicidad y borrado logico. Cada test se revierte. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FinalistaRepositoryTest {

    @Autowired
    private FinalistaRepository finalistaRepository;

    @Autowired
    private ComunicadoRepository comunicadoRepository;

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

    private long postulacion(String ci) {
        long usuario = jdbc.queryForObject("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, ?, ?, '!', 'Ana', 'Rojas' FROM rol WHERE nombre = 'POSTULANTE' RETURNING id""",
                Long.class, ci, ci + "@prueba.test");
        jdbc.update("""
                INSERT INTO postulante (usuario_id, carrera_id, registro_universitario, telefono)
                VALUES (?, (SELECT min(id) FROM carrera), ?, '70000000')""", usuario, "RU-" + ci);
        long gestion = jdbc.queryForObject("""
                INSERT INTO gestion_academica (etiqueta, fecha_inicio, fecha_fin)
                VALUES (?, DATE '2026-07-01', DATE '2026-12-31') RETURNING id""", Long.class, "G-" + ci);
        long convocatoria = jdbc.queryForObject("""
                INSERT INTO convocatoria (gestion_id, nombre, fecha_apertura, fecha_cierre, estado)
                VALUES (?, 'Conv', '2026-07-01T00:00:00Z', '2026-08-31T00:00:00Z',
                        CAST('CERRADA' AS estado_convocatoria)) RETURNING id""", Long.class, gestion);
        return jdbc.queryForObject("""
                INSERT INTO postulacion (convocatoria_id, postulante_id, estado, numero_envios)
                VALUES (?, ?, CAST('calificada' AS estado_postulacion), 1) RETURNING id""", Long.class,
                convocatoria, usuario);
    }

    @Test
    void finalista_persisteSusFechasYSeBuscaPorCodigo() {
        long postulacion = postulacion("9000001");
        Finalista guardado = finalistaRepository.saveAndFlush(new Finalista(postulacion, 3));
        guardado.setPlazoEntrega(LocalDate.of(2026, 12, 1));
        guardado.setNotificadoEn(Instant.parse("2026-11-06T10:00:00Z"));
        finalistaRepository.saveAndFlush(guardado);

        Finalista leido = finalistaRepository.findByCodigo(guardado.getCodigo()).orElseThrow();
        assertThat(leido.getPosicion()).isEqualTo(3);
        assertThat(leido.getPlazoEntrega()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(leido.getNotificadoEn()).isEqualTo(Instant.parse("2026-11-06T10:00:00Z"));
        assertThat(leido.getDocumentacionRecibidaEn()).isNull();
        assertThat(finalistaRepository.findByCodigoIn(List.of(guardado.getCodigo(), UUID.randomUUID()))).hasSize(1);
    }

    @Test
    void finalista_unaPostulacionNoPuedeSerFinalistaDosVeces() {
        long postulacion = postulacion("9000002");
        finalistaRepository.saveAndFlush(new Finalista(postulacion, 1));

        assertThatThrownBy(() -> finalistaRepository.saveAndFlush(new Finalista(postulacion, 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void finalista_elUnicoEsParcial_conElPrimeroEliminadoSePuedeVolverAElegir() {
        long postulacion = postulacion("9000004");
        jdbc.update("INSERT INTO finalista (postulacion_id, posicion) VALUES (?, 1)", postulacion);
        jdbc.update("UPDATE finalista SET eliminado_en = now() WHERE postulacion_id = ?", postulacion);

        jdbc.update("INSERT INTO finalista (postulacion_id, posicion) VALUES (?, 2)", postulacion);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM finalista WHERE postulacion_id = ?", Long.class,
                postulacion)).isEqualTo(2);
    }

    @Test
    void finalista_eliminadoLogicamenteNoSeLee() {
        Finalista guardado = finalistaRepository.saveAndFlush(new Finalista(postulacion("9000003"), 1));
        jdbc.update("UPDATE finalista SET eliminado_en = now() WHERE id = ?", guardado.getId());

        assertThat(finalistaRepository.findByCodigo(guardado.getCodigo())).isEmpty();
    }

    @Test
    void comunicado_persisteElEnumNativoYSeBuscaPorCodigo() {
        long admin = jdbc.queryForObject("""
                INSERT INTO usuario (rol_id, ci, correo, password_hash, nombres, apellidos)
                SELECT id, '9000009', 'admin9@prueba.test', '!', 'Admin', 'DUBSS' FROM rol
                WHERE nombre = 'ADMINISTRADOR_DUBSS' RETURNING id""", Long.class);
        Comunicado guardado = comunicadoRepository.saveAndFlush(new Comunicado(null, admin, "Titulo", "Contenido",
                DestinatariosComunicado.FINALISTAS, Instant.parse("2026-11-06T10:00:00Z")));

        Comunicado leido = comunicadoRepository.findByCodigo(guardado.getCodigo()).orElseThrow();
        assertThat(leido.getDestinatarios()).isEqualTo(DestinatariosComunicado.FINALISTAS);
        assertThat(leido.getConvocatoriaId()).isNull();
        assertThat(jdbc.queryForObject("SELECT destinatarios::text FROM comunicado WHERE id = ?", String.class,
                leido.getId())).isEqualTo("FINALISTAS");
    }
}
