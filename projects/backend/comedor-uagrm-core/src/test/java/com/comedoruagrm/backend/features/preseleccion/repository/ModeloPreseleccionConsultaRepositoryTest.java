package com.comedoruagrm.backend.features.preseleccion.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/** Versiones del modelo de preseleccion y sus parametros (CU26) con Postgres real. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ModeloPreseleccionConsultaRepository.class)
class ModeloPreseleccionConsultaRepositoryTest {

    @Autowired
    private ModeloPreseleccionConsultaRepository consulta;

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

    private long admin(String ci) {
        return new ExpedienteFixtures(jdbc).usuario(ci, "ADMINISTRADOR_DUBSS");
    }

    @Test
    void laMigracionSiembraLaVersionV10InactivaConLasDiezPonderacionesQueSuman100() throws Exception {
        // * Otros tests de integración borran todos los modelos al terminar (y con ellos la semilla), así que
        // * se ejecuta el SQL de la migración acá: se prueba el script y no el estado que dejó otro test.
        String sql = new String(new ClassPathResource(
                "db/migration/V20261121100000__sembrar_modelo_preseleccion_v1_0.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        jdbc.update("DELETE FROM parametro_modelo WHERE modelo_id IN "
                + "(SELECT id FROM modelo_preseleccion WHERE version = 'v1.0')");
        jdbc.update("DELETE FROM modelo_preseleccion WHERE version = 'v1.0'");

        jdbc.execute(sql);
        // Idempotente: volver a correrla no duplica nada.
        jdbc.execute(sql);

        var v10 = consulta.listar(1000, 0).stream().filter(m -> m.version().equals("v1.0")).toList();
        assertThat(v10).hasSize(1);
        assertThat(v10.get(0).activo()).isFalse();
        var parametros = consulta.parametros(v10.get(0).id());
        assertThat(parametros).extracting("clave").containsExactly("ingresos_familiares", "dependientes",
                "procedencia", "patrimonio", "estado_vulnerabilidad", "promedio_academico", "dependencia_economica",
                "ocupacion_sosten", "discapacidad", "hijos");
        assertThat(parametros.stream().map(p -> p.ponderacion()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100");
        assertThat(consulta.versiones()).contains("v1.0");
    }

    @Test
    void crear_dejaLaVersionVigenteConSusParametros() {
        long creadoPor = admin("9600001");

        var modelo = consulta.crear(creadoPor, "v9.1", "Primer ajuste");
        consulta.insertarParametro(modelo.id(), "dependientes", new BigDecimal("20.00"));
        consulta.insertarParametro(modelo.id(), "patrimonio", new BigDecimal("80.00"));

        var vigente = consulta.vigente().orElseThrow();
        assertThat(vigente.id()).isEqualTo(modelo.id());
        assertThat(vigente.version()).isEqualTo("v9.1");
        assertThat(vigente.activo()).isTrue();
        assertThat(consulta.parametros(modelo.id())).extracting("clave").containsExactly("dependientes", "patrimonio");
        assertThat(consulta.parametros(modelo.id()).get(0).ponderacion()).isEqualByComparingTo("20");
    }

    @Test
    void archivarVigente_yCrearOtra_dejaUnaSolaVigenteYConservaElHistorial() {
        long creadoPor = admin("9600002");
        var primera = consulta.crear(creadoPor, "v9.2", null);

        consulta.archivarVigente();
        var segunda = consulta.crear(creadoPor, "v9.3", null);

        assertThat(consulta.vigente().orElseThrow().id()).isEqualTo(segunda.id());
        assertThat(consulta.versiones()).contains("v9.2", "v9.3");
        var historial = consulta.listar(10, 0);
        assertThat(historial.stream().map(m -> m.id()).toList()).containsSubsequence(segunda.id(), primera.id());
        assertThat(historial.stream().filter(m -> m.id() == primera.id()).findFirst().orElseThrow().activo())
                .isFalse();
        assertThat(consulta.contar()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void listar_respetaLimiteYDesplazamiento() {
        long creadoPor = admin("9600003");
        consulta.crear(creadoPor, "v9.4", null);
        consulta.archivarVigente();
        consulta.crear(creadoPor, "v9.5", null);

        assertThat(consulta.listar(1, 0)).hasSize(1);
        assertThat(consulta.listar(1, 0).get(0).version()).isEqualTo("v9.5");
        assertThat(consulta.listar(1, 1).get(0).version()).isEqualTo("v9.4");
    }

    @Test
    void insertarParametro_conPonderacionFueraDeRango_violaElCheck() {
        var modelo = consulta.crear(admin("9600004"), "v9.6", null);

        assertThatThrownBy(() -> consulta.insertarParametro(modelo.id(), "dependientes", new BigDecimal("100.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void insertarParametro_conUnaClaveRepetida_violaElIndiceUnico() {
        var modelo = consulta.crear(admin("9600005"), "v9.7", null);
        consulta.insertarParametro(modelo.id(), "dependientes", new BigDecimal("20.00"));

        assertThatThrownBy(() -> consulta.insertarParametro(modelo.id(), "dependientes", new BigDecimal("10.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void crear_otraVigenteSinArchivarLaAnterior_violaElIndiceUnicoDeActivo() {
        long creadoPor = admin("9600006");
        consulta.crear(creadoPor, "v9.8", null);

        assertThatThrownBy(() -> consulta.crear(creadoPor, "v9.9", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
