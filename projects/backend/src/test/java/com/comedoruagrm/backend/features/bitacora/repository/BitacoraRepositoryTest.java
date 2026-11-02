package com.comedoruagrm.backend.features.bitacora.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.comedoruagrm.backend.features.bitacora.BitacoraAcciones;
import com.comedoruagrm.backend.features.bitacora.model.Bitacora;
import com.comedoruagrm.backend.support.PostgresTestDatabase;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BitacoraRepositoryTest {

    @Autowired
    private BitacoraRepository bitacoraRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeAll
    static void saltarSiNoHayPostgres() {
        PostgresTestDatabase.asegurarDisponible();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.registrar(registry);
    }

    @Test
    void guardar_conDetalleEIp_losLeeDeVueltaDesdeJsonbEInet() throws Exception {
        Bitacora guardada = bitacoraRepository.saveAndFlush(new Bitacora(null, BitacoraAcciones.INICIO_SESION,
                "usuario", 5L, Map.of("canal", "web"), InetAddress.getByName("203.0.113.10")));
        entityManager.clear();

        Bitacora leida = bitacoraRepository.findById(guardada.getId()).orElseThrow();

        assertThat(leida.getAccion()).isEqualTo("INICIO_SESION");
        assertThat(leida.getDetalle()).containsEntry("canal", "web");
        assertThat(leida.getIp().getHostAddress()).isEqualTo("203.0.113.10");
        assertThat(leida.getUsuario()).isNull();
    }
}
