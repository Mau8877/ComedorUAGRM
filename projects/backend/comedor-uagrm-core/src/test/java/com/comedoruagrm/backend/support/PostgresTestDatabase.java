package com.comedoruagrm.backend.support;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Postgres real para los tests de integracion (ver TESTING_BACKEND.md).
 *
 * * Un solo contenedor `postgres:18-alpine` por JVM, levantado la primera vez
 * * que un test lo pide y apagado al terminar: los contextos de Spring se
 * * cachean entre clases de test y todos comparten la misma base. Cada test
 * * limpia lo que inserta (o corre dentro de una transaccion que se revierte).
 * * Si Docker no esta disponible, `asegurarDisponible()` saltea el test
 * * (SKIPPED) en vez de romper el build.
 * * Alternativa cuando Testcontainers no conecta (Docker Desktop en Windows):
 * * apuntar a un Postgres ya levantado y VACIO con
 * * -Dtest.postgres.url=jdbc:postgresql://localhost:5432/mi_base_vacia
 * * (-Dtest.postgres.user / -Dtest.postgres.password, por defecto postgres).
 * ! Con esa alternativa, Flyway migra esa base: usar una base descartable,
 * ! nunca la de desarrollo.
 */
public final class PostgresTestDatabase {

    private static final String URL_EXTERNA = System.getProperty("test.postgres.url");

    private static PostgreSQLContainer<?> contenedor;

    private PostgresTestDatabase() {
    }

    /** Llamar desde un @BeforeAll: saltea la clase de test si no hay Postgres disponible. */
    public static void asegurarDisponible() {
        if (URL_EXTERNA != null) {
            return;
        }
        boolean dockerDisponible;
        try {
            dockerDisponible = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            dockerDisponible = false;
        }
        assumeTrue(dockerDisponible,
                "Docker no esta disponible -- se salta en vez de romper el build. "
                        + "Ver TESTING_BACKEND.md#testcontainers.");
    }

    /** Llamar desde un metodo @DynamicPropertySource. */
    public static void registrar(DynamicPropertyRegistry registry) {
        if (URL_EXTERNA != null) {
            registry.add("spring.datasource.url", () -> URL_EXTERNA);
            registry.add("spring.datasource.username", () -> System.getProperty("test.postgres.user", "postgres"));
            registry.add("spring.datasource.password",
                    () -> System.getProperty("test.postgres.password", "postgres"));
            return;
        }
        PostgreSQLContainer<?> postgres = iniciarContenedor();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static synchronized PostgreSQLContainer<?> iniciarContenedor() {
        if (contenedor == null) {
            contenedor = new PostgreSQLContainer<>("postgres:18-alpine");
            contenedor.start();
            Runtime.getRuntime().addShutdownHook(new Thread(contenedor::stop));
        }
        return contenedor;
    }
}
