package com.comedoruagrm.backend.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj de la aplicacion. Se inyecta (en lugar de llamar a `Instant.now()`) donde una prueba necesita
 * simular otra fecha, por ejemplo el cambio de año del numero de acta (CU08).
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
