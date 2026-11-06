package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CanalPushLogTest {

    @Test
    void enviar_daPorEntregadosTodosLosTelefonosYNoMarcaTokensInvalidos() {
        ResultadoPush resultado = new CanalPushLog()
                .enviar(List.of("t1", "t2", "t3"), new MensajePush("Titulo", "Cuerpo", Map.of()));

        assertThat(resultado.enviados()).isEqualTo(3);
        assertThat(resultado.tokensInvalidos()).isEmpty();
        assertThat(resultado.tokensPasajeros()).isEmpty();
    }

    @Test
    void enviar_sinTelefonos_devuelveCeroEnviados() {
        ResultadoPush resultado = new CanalPushLog().enviar(List.of(), new MensajePush("T", "C", Map.of()));

        assertThat(resultado).isEqualTo(ResultadoPush.vacio());
    }
}
