package com.comedoruagrm.backend.features.preseleccion.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class VersionModeloTest {

    @Test
    void siguiente_sinVersiones_esV1Punto0() {
        assertThat(VersionModelo.siguiente(List.of())).isEqualTo("v1.0");
    }

    @Test
    void siguiente_sumaUnoAlMenorDeLaMayorVersion() {
        assertThat(VersionModelo.siguiente(List.of("v1.2", "v1.3", "v1.1"))).isEqualTo("v1.4");
    }

    @Test
    void siguiente_comparaNumericamenteNoComoTexto() {
        assertThat(VersionModelo.siguiente(List.of("v1.9", "v1.10"))).isEqualTo("v1.11");
        assertThat(VersionModelo.siguiente(List.of("v1.99", "v2.0"))).isEqualTo("v2.1");
    }

    @Test
    void siguiente_unaVersionSinPuntoCuentaComoPuntoCero() {
        assertThat(VersionModelo.siguiente(List.of("v1"))).isEqualTo("v1.1");
    }

    @Test
    void siguiente_ignoraLasVersionesQueNoSiguenElFormato() {
        assertThat(VersionModelo.siguiente(List.of("test-123", "modelo-final", "v1.2"))).isEqualTo("v1.3");
        assertThat(VersionModelo.siguiente(List.of("test-123"))).isEqualTo("v1.0");
    }
}
