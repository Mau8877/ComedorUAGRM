package com.comedoruagrm.backend.features.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GeneradorContrasenaTemporalTest {

    private final GeneradorContrasenaTemporal generador = new GeneradorContrasenaTemporal();

    @Test
    void generar_devuelveDoceCaracteres() {
        assertThat(generador.generar()).hasSize(12);
    }

    @Test
    void generar_usaSoloElAlfabetoSinCaracteresQueSeConfunden() {
        for (int i = 0; i < 500; i++) {
            assertThat(generador.generar()).matches("[A-HJ-NP-Za-km-z2-9]{12}");
        }
    }

    @Test
    void generar_siempreTieneAlMenosUnaLetraYUnDigito() {
        for (int i = 0; i < 500; i++) {
            String contrasena = generador.generar();
            assertThat(contrasena).containsPattern("[A-Za-z]").containsPattern("[0-9]");
        }
    }

    @Test
    void generar_milVeces_noRepiteNinguna() {
        Set<String> generadas = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            generadas.add(generador.generar());
        }
        assertThat(generadas).hasSize(1000);
    }

    @Test
    void generar_cumpleLaPoliticaDeContrasenasDelSistema() {
        // CU01: de 6 a 72 caracteres.
        assertThat(generador.generar().length()).isBetween(6, 72);
    }
}
