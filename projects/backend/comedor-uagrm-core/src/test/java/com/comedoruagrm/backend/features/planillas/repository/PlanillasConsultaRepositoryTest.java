package com.comedoruagrm.backend.features.planillas.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.planillas.dto.PlanillaResponse;
import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository.Becado;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Consultas de la planilla de asistencia (CU09) y su migracion, con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanillasConsultaRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    private ExpedienteFixtures fx;
    private PlanillasConsultaRepository consulta;
    private long convocatoria;
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
        consulta = new PlanillasConsultaRepository(new NamedParameterJdbcTemplate(jdbc));
        convocatoria = fx.convocatoria("PCONS");
        admin = fx.usuario("9740001", "ADMINISTRADOR_DUBSS");
        siguienteCi = 9750000;
    }

    private UUID codigo(long convocatoriaId) {
        return jdbc.queryForObject("SELECT codigo_convocatoria FROM convocatoria WHERE id = ?", UUID.class,
                convocatoriaId);
    }

    /** Un postulante con su beca en el estado dado en la convocatoria. */
    private long beca(long convocatoriaId, String nombres, String apellidos, String estado) {
        long usuario = fx.postulante(String.valueOf(siguienteCi++));
        jdbc.update("UPDATE usuario SET nombres = ?, apellidos = ? WHERE id = ?", nombres, apellidos, usuario);
        return jdbc.queryForObject("""
                INSERT INTO beca (convocatoria_id, postulante_id, estado, fecha_inicio)
                VALUES (?, ?, CAST(? AS estado_beca), DATE '2026-09-01') RETURNING id""", Long.class,
                convocatoriaId, usuario, estado);
    }

    private long planilla(long convocatoriaId, String desde, String hasta, int total, String generadaEn) {
        return jdbc.queryForObject("""
                INSERT INTO planilla_asistencia (convocatoria_id, generada_por, fecha_desde, fecha_hasta,
                       total_becados, storage_path, generada_en)
                VALUES (?, ?, CAST(? AS date), CAST(? AS date), ?, 'planillas/x.pdf', CAST(? AS timestamptz))
                RETURNING id""", Long.class, convocatoriaId, admin, desde, hasta, total, generadaEn);
    }

    // ---- convocatoria ----

    @Test
    void convocatoria_existente_devuelveSuGestionYUnaInexistenteVacio() {
        var datos = consulta.convocatoria(codigo(convocatoria)).orElseThrow();

        assertThat(datos.id()).isEqualTo(convocatoria);
        assertThat(datos.nombre()).isEqualTo("Conv PCONS");
        assertThat(datos.gestion()).isEqualTo("G-PCONS");
        assertThat(datos.gestionDesde()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(datos.gestionHasta()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(consulta.convocatoria(UUID.randomUUID())).isEmpty();
    }

    @Test
    void convocatoria_eliminada_noSeEncuentra() {
        jdbc.update("UPDATE convocatoria SET eliminado_en = now() WHERE id = ?", convocatoria);

        assertThat(consulta.convocatoria(codigo(convocatoria))).isEmpty();
    }

    // ---- becados vigentes ----

    @Test
    void becadosVigentes_soloLosVigentesDeLaConvocatoria_ordenadosPorApellidoYNombre() {
        long otra = fx.convocatoria("POTRA");
        beca(convocatoria, "Zoe", "Mamani", "VIGENTE");
        beca(convocatoria, "Ana", "Mamani", "VIGENTE");
        beca(convocatoria, "Luis", "Arce", "VIGENTE");
        beca(convocatoria, "Baja", "Suarez", "BAJA");
        beca(convocatoria, "Susp", "Suarez", "SUSPENDIDA");
        beca(otra, "Otra", "Convocatoria", "VIGENTE");

        List<Becado> becados = consulta.becadosVigentes(convocatoria);

        assertThat(becados).extracting(b -> b.apellidos() + " " + b.nombres())
                .containsExactly("Arce Luis", "Mamani Ana", "Mamani Zoe");
        assertThat(becados).allSatisfy(b -> {
            assertThat(b.ci()).startsWith("975");
            assertThat(b.carrera()).isNotBlank();
        });
        assertThat(consulta.contarVigentes(convocatoria)).isEqualTo(3);
        assertThat(consulta.contarVigentes(otra)).isEqualTo(1);
    }

    @Test
    void becadosVigentes_ignoraLasBecasYLosUsuariosEliminados() {
        long eliminada = beca(convocatoria, "Eli", "Minada", "VIGENTE");
        beca(convocatoria, "Ana", "Activa", "VIGENTE");
        jdbc.update("UPDATE beca SET eliminado_en = now() WHERE id = ?", eliminada);

        assertThat(consulta.becadosVigentes(convocatoria)).extracting(Becado::apellidos).containsExactly("Activa");
        assertThat(consulta.contarVigentes(convocatoria)).isEqualTo(1);
    }

    @Test
    void becadosVigentes_sinBecas_devuelveVacioYCero() {
        assertThat(consulta.becadosVigentes(convocatoria)).isEmpty();
        assertThat(consulta.contarVigentes(convocatoria)).isZero();
    }

    // ---- planillas ----

    @Test
    void planillas_laMasRecientePrimero_conSuPeriodoTotalYQuienLaGenero() {
        planilla(convocatoria, "2026-08-01", "2026-08-31", 884, "2026-07-30T15:00:00Z");
        planilla(convocatoria, "2026-09-01", "2026-09-30", 880, "2026-08-30T15:00:00Z");

        Page<PlanillaResponse> pagina = consulta.planillas(codigo(convocatoria), PageRequest.of(0, 20));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(PlanillaResponse::totalBecados).containsExactly(880, 884);
        PlanillaResponse primera = pagina.getContent().get(0);
        assertThat(primera.convocatoria()).isEqualTo(codigo(convocatoria));
        assertThat(primera.fechaDesde()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(primera.fechaHasta()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(primera.generadaPor()).isNotBlank();
        assertThat(primera.codigo()).isNotNull();
    }

    @Test
    void planillas_soloLasDeLaConvocatoria_ySinLasEliminadas() {
        long otra = fx.convocatoria("POTRA");
        planilla(convocatoria, "2026-08-01", "2026-08-31", 10, "2026-07-30T15:00:00Z");
        long eliminada = planilla(convocatoria, "2026-09-01", "2026-09-30", 11, "2026-08-30T15:00:00Z");
        planilla(otra, "2026-09-01", "2026-09-30", 99, "2026-08-30T15:00:00Z");
        jdbc.update("UPDATE planilla_asistencia SET eliminado_en = now() WHERE id = ?", eliminada);

        Page<PlanillaResponse> pagina = consulta.planillas(codigo(convocatoria), PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(PlanillaResponse::totalBecados).containsExactly(10);
    }

    @Test
    void planillas_pagina_yOrdenaPorFechaDesdeConDesempate() {
        planilla(convocatoria, "2026-08-01", "2026-08-31", 1, "2026-07-30T15:00:00Z");
        planilla(convocatoria, "2026-10-01", "2026-10-31", 3, "2026-07-31T15:00:00Z");
        planilla(convocatoria, "2026-09-01", "2026-09-30", 2, "2026-08-01T15:00:00Z");

        Page<PlanillaResponse> ascendente = consulta.planillas(codigo(convocatoria),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "fechaDesde")));
        Page<PlanillaResponse> segunda = consulta.planillas(codigo(convocatoria),
                PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "fechaDesde")));

        assertThat(ascendente.getTotalElements()).isEqualTo(3);
        assertThat(ascendente.getTotalPages()).isEqualTo(2);
        assertThat(ascendente.getContent()).extracting(PlanillaResponse::totalBecados).containsExactly(1, 2);
        assertThat(segunda.getContent()).extracting(PlanillaResponse::totalBecados).containsExactly(3);
    }

    @Test
    void planillas_sortNoSoportado_esErrSys05() {
        UUID codigo = codigo(convocatoria);

        assertThatThrownBy(() -> consulta.planillas(codigo, PageRequest.of(0, 20, Sort.by("storagePath"))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_05"));
        assertThatThrownBy(() -> consulta.planillas(codigo, PageRequest.of(0, 20, Sort.by("generadaEn", "fechaDesde"))))
                .isInstanceOf(ValidationException.class);
    }

    // ---- migracion ----

    @Test
    void migracion_rechazaUnPeriodoInvertido() {
        assertThatThrownBy(() -> planilla(convocatoria, "2026-09-30", "2026-09-01", 5, "2026-08-30T15:00:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Un test por restriccion: tras una violacion Postgres aborta la transaccion y la siguiente sentencia fallaria
    // por eso y no por su restriccion.
    @Test
    void migracion_rechazaUnTotalEnCero() {
        assertThatThrownBy(() -> planilla(convocatoria, "2026-09-01", "2026-09-30", 0, "2026-08-30T15:00:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void migracion_permiteVariasPlanillasDelMismoPeriodo() {
        planilla(convocatoria, "2026-09-01", "2026-09-30", 5, "2026-08-30T15:00:00Z");
        planilla(convocatoria, "2026-09-01", "2026-09-30", 6, "2026-08-31T15:00:00Z");

        assertThat(consulta.planillas(codigo(convocatoria), PageRequest.of(0, 20)).getTotalElements()).isEqualTo(2);
    }
}
