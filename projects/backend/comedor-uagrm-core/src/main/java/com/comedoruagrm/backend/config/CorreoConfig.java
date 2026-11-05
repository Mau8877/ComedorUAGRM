package com.comedoruagrm.backend.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Envio de correo asincrono (CU15).
 *
 * * El envio corre en un ejecutor propio y chico: asi la peticion que lo pide
 * * no espera al SMTP, y su tiempo de respuesta no delata si la cuenta existe.
 */
@Configuration
@EnableAsync
public class CorreoConfig {

    /** Nombre del ejecutor que usan los metodos `@Async` de correo. */
    public static final String EJECUTOR_CORREO = "ejecutorCorreo";

    @Bean(name = EJECUTOR_CORREO)
    public Executor ejecutorCorreo() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(2);
        ejecutor.setQueueCapacity(100);
        ejecutor.setThreadNamePrefix("correo-");
        ejecutor.initialize();
        return ejecutor;
    }
}
