package com.comedoruagrm.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas programadas (`@Scheduled`). Hoy solo hay una: el refresco nocturno de la vista de indicadores
 * del dashboard (CU12).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
