package com.comedoruagrm.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.comedoruagrm.backend.config.SchedulingConfig;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** El refresco por lote de la vista de indicadores (CU12) con el `JdbcTemplate` simulado. */
class RefrescoIndicadoresServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RefrescoIndicadoresService service = new RefrescoIndicadoresService(jdbc);

    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void escucharLogs() {
        logger = (Logger) LoggerFactory.getLogger(RefrescoIndicadoresService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void dejarDeEscuchar() {
        logger.detachAppender(logs);
    }

    @Test
    void refrescar_usaRefreshConcurrently_yLoguaLaDuracion() {
        boolean salioBien = service.refrescar();

        assertThat(salioBien).isTrue();
        verify(jdbc).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_indicadores_convocatoria");
        assertThat(logs.list).hasSize(1);
        assertThat(logs.list.get(0).getLevel().toString()).isEqualTo("INFO");
        assertThat(logs.list.get(0).getArgumentArray()).hasSize(2);
    }

    @Test
    void refrescar_siFallaLoguaErrorSinPropagar_yDevuelveFalse() {
        doThrow(new DataAccessResourceFailureException("base caida: usuario=postgres")).when(jdbc)
                .execute(anyString());

        boolean salioBien = service.refrescar();

        assertThat(salioBien).isFalse();
        assertThat(logs.list).hasSize(1);
        ILoggingEvent evento = logs.list.get(0);
        assertThat(evento.getLevel().toString()).isEqualTo("ERROR");
        // Solo el tipo del error: nunca el mensaje, que podria traer datos.
        String registrado = evento.getFormattedMessage() + java.util.Arrays.toString(evento.getArgumentArray());
        assertThat(registrado).contains("DataAccessResourceFailureException").doesNotContain("postgres");
    }

    @Test
    void refrescarSiEstaVacia_conLaVistaVacia_haceUnRefreshNormal() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);

        boolean refresco = service.refrescarSiEstaVacia();

        assertThat(refresco).isTrue();
        verify(jdbc).execute("REFRESH MATERIALIZED VIEW mv_indicadores_convocatoria");
        verify(jdbc, never()).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_indicadores_convocatoria");
    }

    @Test
    void refrescarSiEstaVacia_conDatos_noHaceNada() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(7L);

        boolean refresco = service.refrescarSiEstaVacia();

        assertThat(refresco).isFalse();
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void refrescarSiEstaVacia_siFallaRevisarLaVista_noPropagaYNoRefresca() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenThrow(new DataAccessResourceFailureException("x"));

        assertThat(service.refrescarSiEstaVacia()).isFalse();

        verify(jdbc, never()).execute(anyString());
        assertThat(logs.list.get(0).getLevel().toString()).isEqualTo("ERROR");
    }

    @Test
    void elJob_programaElRefrescoDeLas02_enLaPaz_conElCronDeLaPropiedad() throws NoSuchMethodException {
        Method metodo = RefrescoIndicadoresJob.class.getMethod("refrescarCadaNoche");

        Scheduled programado = metodo.getAnnotation(Scheduled.class);

        assertThat(programado.cron()).isEqualTo("${app.dashboard.refresco-cron:0 0 2 * * *}");
        assertThat(programado.zone()).isEqualTo("America/La_Paz");
    }

    @Test
    void elJob_alArrancarRefrescaSoloSiLaVistaEstaVacia_yElCronNocturnoRefrescaConcurrentemente() throws Exception {
        RefrescoIndicadoresService simulado = mock(RefrescoIndicadoresService.class);
        RefrescoIndicadoresJob job = new RefrescoIndicadoresJob(simulado);

        job.alArrancar();
        job.refrescarCadaNoche();

        verify(simulado).refrescarSiEstaVacia();
        verify(simulado).refrescar();
        assertThat(RefrescoIndicadoresJob.class.getMethod("alArrancar").getAnnotation(EventListener.class).value())
                .containsExactly(ApplicationReadyEvent.class);
    }

    @Test
    void lasTareasProgramadasEstanActivadas() {
        assertThat(SchedulingConfig.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
        assertThat(List.of(RefrescoIndicadoresJob.class.getDeclaredMethods())).isNotEmpty();
    }
}
