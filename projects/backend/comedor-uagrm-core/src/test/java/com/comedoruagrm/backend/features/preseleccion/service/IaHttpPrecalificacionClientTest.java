package com.comedoruagrm.backend.features.preseleccion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.preseleccion.dto.PuntajeCalculado;
import com.comedoruagrm.backend.features.preseleccion.dto.SolicitudPuntaje;
import com.comedoruagrm.backend.features.preseleccion.exception.IaDatosInvalidosException;
import com.comedoruagrm.backend.features.preseleccion.exception.IaNoDisponibleException;
import com.sun.net.httpserver.HttpServer;

/**
 * El cliente HTTP del modulo de IA contra un servidor falso (el de la JDK): como lo interpreta segun lo que
 * responde la IA (CU03: A1, A2 y A6).
 */
class IaHttpPrecalificacionClientTest {

    private HttpServer servidor;
    private volatile int estado;
    private volatile String cuerpo;
    private volatile long demoraMs;
    private volatile String cuerpoRecibido;

    @BeforeEach
    void levantar() throws IOException {
        estado = 200;
        cuerpo = "";
        demoraMs = 0;
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/v1/puntaje", exchange -> {
            cuerpoRecibido = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        servidor.start();
    }

    @AfterEach
    void apagar() {
        servidor.stop(0);
    }

    private IaHttpPrecalificacionClient cliente(long timeoutSegundos) {
        return new IaHttpPrecalificacionClient("http://127.0.0.1:" + servidor.getAddress().getPort(),
                timeoutSegundos);
    }

    private static SolicitudPuntaje solicitud() {
        return new SolicitudPuntaje(UUID.randomUUID(), new BigDecimal("1800.00"), 4, "Provincia",
                "Ing. Informática", "Sin vivienda propia", "Hogar monoparental", new BigDecimal("72.50"), "v1.4", null,
                "Solo de padre o madre", "Comerciante minorista", false, null, false);
    }

    @Test
    void calcular_conUnaRespuestaValida_devuelveElPuntajeLaVersionYElDetalle() {
        cuerpo = "{\"puntaje\": 55.67, \"modelo\": \"v1.4\", \"detalle\": {\"variables\": {\"hijos\": {\"puntos\": 0}}}}";

        PuntajeCalculado r = cliente(5).calcular(solicitud());

        assertThat(r.puntaje()).isEqualByComparingTo("55.67");
        assertThat(r.modelo()).isEqualTo("v1.4");
        assertThat(r.detalle()).containsKey("variables");
    }

    @Test
    void calcular_mandaLasVariablesEnCamelCaseYNuncaDatosPersonales() {
        cuerpo = "{\"puntaje\": 10, \"modelo\": \"v1.4\", \"detalle\": {}}";

        cliente(5).calcular(solicitud());

        assertThat(cuerpoRecibido).contains("\"ingresosFamiliares\"", "\"estadoVulnerabilidad\"",
                "\"dependenciaEconomica\":\"Solo de padre o madre\"", "\"tieneHijos\":false");
        assertThat(cuerpoRecibido).doesNotContain("nombres", "apellidos", "\"ci\"", "correo");
    }

    @Test
    void calcular_unaRutaQueLaIaNoTiene404_esQueNoEstaDisponible() {
        estado = 404;

        assertThatThrownBy(() -> cliente(5).calcular(solicitud())).isInstanceOf(IaNoDisponibleException.class);
    }

    @Test
    void calcular_unErrorDelServidorDeLaIa_esQueNoEstaDisponible() {
        estado = 500;

        assertThatThrownBy(() -> cliente(5).calcular(solicitud())).isInstanceOf(IaNoDisponibleException.class);
    }

    @Test
    void calcular_siLaIaExcedeElTiempoDeEspera_esQueNoEstaDisponible() {
        cuerpo = "{\"puntaje\": 10, \"modelo\": \"v1.4\", \"detalle\": {}}";
        demoraMs = 2500;

        assertThatThrownBy(() -> cliente(1).calcular(solicitud())).isInstanceOf(IaNoDisponibleException.class);
    }

    @Test
    void calcular_siNadieEscuchaEnLaDireccion_esQueNoEstaDisponible() {
        IaHttpPrecalificacionClient sinServidor = new IaHttpPrecalificacionClient("http://127.0.0.1:1", 1);

        assertThatThrownBy(() -> sinServidor.calcular(solicitud())).isInstanceOf(IaNoDisponibleException.class);
    }

    @Test
    void calcular_siLaIaRechazaLosDatos422_esUnDatoInvalido() {
        estado = 422;
        cuerpo = "{\"detail\": \"Variables desconocidas: x\"}";

        assertThatThrownBy(() -> cliente(5).calcular(solicitud())).isInstanceOf(IaDatosInvalidosException.class);
    }

    @Test
    void calcular_conUnPuntajeFueraDeRangoOSinVersion_esUnDatoInvalido() {
        for (String respuesta : new String[] {
            "{\"puntaje\": 70.01, \"modelo\": \"v1.4\", \"detalle\": {}}",
            "{\"puntaje\": -1, \"modelo\": \"v1.4\", \"detalle\": {}}",
            "{\"puntaje\": 40, \"detalle\": {}}",
            "{\"modelo\": \"v1.4\", \"detalle\": {}}"}) {
            cuerpo = respuesta;

            assertThatThrownBy(() -> cliente(5).calcular(solicitud())).as(respuesta)
                    .isInstanceOf(IaDatosInvalidosException.class);
        }
    }

    @Test
    void calcular_conElTopeExactoDe70_loAcepta() {
        cuerpo = "{\"puntaje\": 70, \"modelo\": \"v1.0\", \"detalle\": {}}";

        assertThat(cliente(5).calcular(solicitud()).puntaje()).isEqualByComparingTo("70");
    }
}
