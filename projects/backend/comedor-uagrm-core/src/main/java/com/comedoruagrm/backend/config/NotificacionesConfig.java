package com.comedoruagrm.backend.config;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.comedoruagrm.backend.features.notificaciones.service.Reintentos;

/**
 * Infraestructura de los envios de notificaciones (CU-EXTRA-01): un ejecutor propio y la
 * politica de reintentos.
 *
 * * El ejecutor no usa `CallerRunsPolicy`: si la cola se llena, el envio se descarta y se
 * * registra, para no frenar (ni hacer fallar) la operacion de negocio que notifico.
 */
@Configuration
public class NotificacionesConfig {

    /** Nombre del ejecutor que usan los metodos `@Async` de notificaciones. */
    public static final String EJECUTOR_NOTIFICACIONES = "ejecutorNotificaciones";

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionesConfig.class);

    @Bean(name = EJECUTOR_NOTIFICACIONES)
    public Executor ejecutorNotificaciones() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(4);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(500);
        ejecutor.setThreadNamePrefix("notif-");
        ejecutor.setRejectedExecutionHandler((tarea, pool) ->
                LOG.warn("Cola de notificaciones llena: se descarta un envio", kv("module", "notificaciones")));
        ejecutor.initialize();
        return ejecutor;
    }

    @Bean
    public Reintentos reintentosNotificaciones(
            @Value("${app.notificaciones.reintentos}") int intentos,
            @Value("${app.notificaciones.esperas-ms}") long[] esperasMs) {
        return new Reintentos(intentos, esperasMs);
    }
}
