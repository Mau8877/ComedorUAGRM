package com.comedoruagrm.backend.features.notificaciones.service;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Reintentos con espera creciente para los envios de push y de correo (CU-EXTRA-01).
 *
 * * Corre en el hilo del ejecutor de notificaciones: dormir aca no demora a nadie mas.
 * * Es en memoria: si el backend se reinicia en medio de una espera, ese envio se pierde (el
 * * aviso sigue en la bandeja). La cantidad de intentos y las esperas vienen de la configuracion.
 */
public class Reintentos {

    private final int intentos;
    private final long[] esperasMs;

    /**
     * @param intentos  intentos totales (el primero incluido), al menos 1
     * @param esperasMs espera antes de cada reintento; si hay menos que reintentos, se repite la ultima
     */
    public Reintentos(int intentos, long[] esperasMs) {
        this.intentos = Math.max(1, intentos);
        this.esperasMs = esperasMs.length == 0 ? new long[] {0} : esperasMs.clone();
    }

    public int intentos() {
        return intentos;
    }

    /**
     * Ejecuta la accion y la repite si lanza un error reintentable, hasta agotar los intentos.
     * Un error no reintentable, o el ultimo, se relanza tal cual.
     */
    public <T> T ejecutar(Supplier<T> accion, Predicate<RuntimeException> reintentable) {
        for (int intento = 1;; intento++) {
            try {
                return accion.get();
            } catch (RuntimeException e) {
                if (intento >= intentos || !reintentable.test(e) || !esperar(intento)) {
                    throw e;
                }
            }
        }
    }

    /**
     * Espera antes de volver a intentar despues del intento fallido numero {@code intentoFallido}
     * (1 el primero). Devuelve false si el hilo fue interrumpido y no se debe seguir.
     */
    public boolean esperar(int intentoFallido) {
        long espera = esperasMs[Math.min(intentoFallido - 1, esperasMs.length - 1)];
        if (espera <= 0) {
            return true;
        }
        try {
            Thread.sleep(espera);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
