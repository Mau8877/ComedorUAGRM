package com.comedoruagrm.backend.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Ejecutor de la precalificacion con IA (CU03). Un pool chico con cola larga: ante un pico de envios (cierre de
 * la convocatoria) las solicitudes se encolan y se procesan en orden sin frenar a quien envia (A5). Si la cola se
 * llena, la solicitud se descarta y el reintento programado recoge la postulacion, que sigue `pendiente`.
 */
@Configuration
public class PreseleccionConfig {

    /** Nombre del ejecutor que usan los metodos `@Async` de la precalificacion. */
    public static final String EJECUTOR_PRECALIFICACION = "ejecutorPrecalificacion";

    @Bean(EJECUTOR_PRECALIFICACION)
    public Executor ejecutorPrecalificacion() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(5000);
        ejecutor.setThreadNamePrefix("precalificacion-");
        ejecutor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy());
        return ejecutor;
    }
}
