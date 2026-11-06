package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ReintentosTest {

    /** Sin esperas reales: el test no puede tardar segundos. */
    private final Reintentos reintentos = new Reintentos(3, new long[] {0, 0});

    @Test
    void ejecutar_conExitoAlPrimerIntento_noReintenta() {
        AtomicInteger llamadas = new AtomicInteger();

        String resultado = reintentos.ejecutar(() -> {
            llamadas.incrementAndGet();
            return "ok";
        }, e -> true);

        assertThat(resultado).isEqualTo("ok");
        assertThat(llamadas).hasValue(1);
    }

    @Test
    void ejecutar_siFallaDosVecesYLaTerceraFunciona_devuelveElResultado() {
        AtomicInteger llamadas = new AtomicInteger();

        String resultado = reintentos.ejecutar(() -> {
            if (llamadas.incrementAndGet() < 3) {
                throw new IllegalStateException("pasajero");
            }
            return "ok";
        }, e -> true);

        assertThat(resultado).isEqualTo("ok");
        assertThat(llamadas).hasValue(3);
    }

    @Test
    void ejecutar_siSiempreFalla_agotaLosIntentosYRelanzaElUltimoError() {
        AtomicInteger llamadas = new AtomicInteger();

        assertThatThrownBy(() -> reintentos.ejecutar(() -> {
            throw new IllegalStateException("error " + llamadas.incrementAndGet());
        }, e -> true)).isInstanceOf(IllegalStateException.class).hasMessage("error 3");

        assertThat(llamadas).hasValue(3);
    }

    @Test
    void ejecutar_conErrorNoReintentable_cortaDeInmediato() {
        AtomicInteger llamadas = new AtomicInteger();

        assertThatThrownBy(() -> reintentos.ejecutar(() -> {
            llamadas.incrementAndGet();
            throw new IllegalArgumentException("definitivo");
        }, e -> e instanceof IllegalStateException)).isInstanceOf(IllegalArgumentException.class);

        assertThat(llamadas).hasValue(1);
    }

    @Test
    void intentos_nuncaEsMenorQueUno() {
        assertThat(new Reintentos(0, new long[] {0}).intentos()).isEqualTo(1);
        assertThat(new Reintentos(3, new long[] {}).intentos()).isEqualTo(3);
    }

    @Test
    void esperar_conEsperaConfigurada_duermeAlmenosEseTiempo() {
        Reintentos conEspera = new Reintentos(3, new long[] {30, 60});

        long inicio = System.nanoTime();
        boolean sigue = conEspera.esperar(1);
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(sigue).isTrue();
        assertThat(ms).isGreaterThanOrEqualTo(25);
    }

    @Test
    void esperar_siElHiloEstaInterrumpido_devuelveFalse() {
        Reintentos conEspera = new Reintentos(3, new long[] {1000});
        Thread.currentThread().interrupt();

        boolean sigue = conEspera.esperar(1);

        assertThat(sigue).isFalse();
        assertThat(Thread.interrupted()).isTrue(); // limpia la marca para no afectar otros tests
    }
}
