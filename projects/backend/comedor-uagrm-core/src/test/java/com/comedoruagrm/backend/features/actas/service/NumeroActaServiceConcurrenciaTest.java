package com.comedoruagrm.backend.features.actas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.support.ExpedienteFixtures;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

/**
 * Numero de acta (CU08) con Postgres real y transacciones de verdad: dos generaciones simultaneas de
 * convocatorias distintas reciben numeros distintos, y la unicidad de `numero` es la red de seguridad.
 */
@SpringBootTest
class NumeroActaServiceConcurrenciaTest {

    // Un año lejano: ninguna otra prueba ni dato de desarrollo usa numeros de ese año.
    private static final Instant ANIO_DE_PRUEBA = Instant.parse("2041-06-01T15:00:00Z");

    @Autowired
    private NumeroActaService service;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transacciones;

    @MockitoBean
    private AlmacenamientoService almacenamiento;

    private long admin;
    private long convocatoriaA;
    private long convocatoriaB;

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
        borrar();
        ExpedienteFixtures fx = new ExpedienteFixtures(jdbc);
        admin = fx.usuario("9740001", "ADMINISTRADOR_DUBSS");
        convocatoriaA = fx.convocatoria("NUMA");
        convocatoriaB = fx.convocatoria("NUMB");
    }

    @AfterEach
    void limpiar() {
        borrar();
    }

    private void borrar() {
        jdbc.update("DELETE FROM acta_adjudicacion");
        jdbc.update("DELETE FROM convocatoria");
        jdbc.update("DELETE FROM gestion_academica");
        jdbc.update("DELETE FROM usuario");
    }

    /** Asigna un numero y registra el acta en una transaccion; `pausaMs` alarga el bloqueo para forzar el choque. */
    private String generar(long convocatoria, long pausaMs) {
        return new TransactionTemplate(transacciones).execute(estado -> {
            String numero = service.siguiente(ANIO_DE_PRUEBA);
            try {
                Thread.sleep(pausaMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            jdbc.update("INSERT INTO acta_adjudicacion (convocatoria_id, numero, storage_path, generada_por) "
                    + "VALUES (?, ?, 'actas/x.pdf', ?)", convocatoria, numero, admin);
            return numero;
        });
    }

    @Test
    void siguiente_conDosGeneracionesSimultaneas_danNumerosDistintosYConsecutivos() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<String>> futuros = new ArrayList<>();
            for (long convocatoria : new long[] {convocatoriaA, convocatoriaB}) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return generar(convocatoria, 400);
                }));
            }
            salida.countDown();

            List<String> numeros = new ArrayList<>();
            for (Future<String> futuro : futuros) {
                numeros.add(futuro.get(30, TimeUnit.SECONDS));
            }

            assertThat(numeros).containsExactlyInAnyOrder("001/2041", "002/2041");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void siguiente_despuesDeUnaActaRegistrada_sigueElCorrelativo() {
        assertThat(generar(convocatoriaA, 0)).isEqualTo("001/2041");
        assertThat(generar(convocatoriaB, 0)).isEqualTo("002/2041");
    }

    @Test
    void siguiente_siLaGeneracionFalla_noQuedaHuecoYElSiguienteIntentoRepiteElNumero() {
        assertThatThrownBy(() -> new TransactionTemplate(transacciones).execute(estado -> {
            service.siguiente(ANIO_DE_PRUEBA);
            throw new IllegalStateException("falla despues de asignar el numero");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(generar(convocatoriaA, 0)).isEqualTo("001/2041");
    }

    @Test
    void unicidad_dosActasConElMismoNumero_laBaseLaRechaza() {
        generar(convocatoriaA, 0);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO acta_adjudicacion (convocatoria_id, numero, "
                + "storage_path, generada_por) VALUES (?, '001/2041', 'actas/y.pdf', ?)", convocatoriaB, admin))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
