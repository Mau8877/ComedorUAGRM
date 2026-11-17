package com.comedoruagrm.backend.features.preseleccion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.service.CalificacionPostulacionService;
import com.comedoruagrm.backend.features.preseleccion.dto.ModeloAplicable;
import com.comedoruagrm.backend.features.preseleccion.dto.PuntajeCalculado;
import com.comedoruagrm.backend.features.preseleccion.dto.SolicitudPuntaje;
import com.comedoruagrm.backend.features.preseleccion.exception.IaDatosInvalidosException;
import com.comedoruagrm.backend.features.preseleccion.exception.IaNoDisponibleException;
import com.comedoruagrm.backend.features.preseleccion.model.ModeloPreseleccion;
import com.comedoruagrm.backend.features.preseleccion.model.PuntajePostulacion;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository.ModeloFila;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository.ParametroFila;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository.DatosPuntaje;
import com.comedoruagrm.backend.features.preseleccion.repository.PuntajePostulacionRepository;
import com.comedoruagrm.backend.features.preseleccion.service.PrecalificacionService.Resultado;

import jakarta.persistence.EntityManager;

class PrecalificacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-07-16T15:00:00Z");

    private final PrecalificacionConsultaRepository consulta = mock(PrecalificacionConsultaRepository.class);
    private final PrecalificacionClient cliente = mock(PrecalificacionClient.class);
    private final ModeloPreseleccionRepository modelos = mock(ModeloPreseleccionRepository.class);
    private final ModeloPreseleccionConsultaRepository modelosConsulta =
            mock(ModeloPreseleccionConsultaRepository.class);
    private final PuntajePostulacionRepository puntajes = mock(PuntajePostulacionRepository.class);
    private final CalificacionPostulacionService calificacion = mock(CalificacionPostulacionService.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    private final PrecalificacionService service;

    private final UUID codigo = UUID.randomUUID();
    private final DatosPuntaje datos = new DatosPuntaje(codigo, new BigDecimal("1800.00"), 4, "Provincia",
            "Ing. Informática", "Sin vivienda propia", "Hogar monoparental", new BigDecimal("72.50"));
    private final ModeloPreseleccion modelo = mock(ModeloPreseleccion.class);

    PrecalificacionServiceTest() {
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new PrecalificacionService(consulta, cliente, modelos, modelosConsulta, puntajes, calificacion,
                new TransactionTemplate(tm), entityManager, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void precalificar_conLaIaDisponible_registraElPuntajeYCalificaLaPostulacion() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(datos));
        when(modelosConsulta.vigente()).thenReturn(Optional.of(new ModeloFila(7L, UUID.randomUUID(), "v1", null, true,
                AHORA)));
        when(modelosConsulta.parametros(7L)).thenReturn(List.of(new ParametroFila("dependientes",
                new BigDecimal("20.00"))));
        when(cliente.calcular(any())).thenReturn(new PuntajeCalculado(new BigDecimal("63.40"), "v1",
                Map.of("ingresos", 20)));
        when(consulta.sigueEnPendiente(40L)).thenReturn(true);
        when(modelos.findByVersion("v1")).thenReturn(Optional.of(modelo));
        when(entityManager.getReference(Postulacion.class, 40L)).thenReturn(mock(Postulacion.class));

        Resultado resultado = service.precalificar(40L);

        assertThat(resultado).isEqualTo(Resultado.CALIFICADA);
        ArgumentCaptor<SolicitudPuntaje> enviada = ArgumentCaptor.forClass(SolicitudPuntaje.class);
        verify(cliente).calcular(enviada.capture());
        assertThat(enviada.getValue().postulacion()).isEqualTo(codigo);
        assertThat(enviada.getValue().carrera()).isEqualTo("Ing. Informática");
        assertThat(enviada.getValue().modelo()).isEqualTo("v1");
        assertThat(enviada.getValue().parametros()).containsEntry("dependientes", new BigDecimal("20.00"));
        ArgumentCaptor<PuntajePostulacion> guardado = ArgumentCaptor.forClass(PuntajePostulacion.class);
        verify(consulta).retirarVigentes(40L);
        verify(puntajes).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getPuntajeTotal()).isEqualByComparingTo("63.40");
        assertThat(guardado.getValue().isVigente()).isTrue();
        assertThat(guardado.getValue().getFechaCalculo()).isEqualTo(AHORA);
        verify(calificacion).marcarCalificada(40L);
    }

    @Test
    void precalificar_mandaALaIaLosDatosDelFormularioOficialDeLaDubss() {
        DatosPuntaje conFormularioOficial = new DatosPuntaje(codigo, new BigDecimal("1800.00"), 4, "Provincia",
                "Ing. Informática", "Sin vivienda propia", "Hogar monoparental", new BigDecimal("72.50"),
                "Independiente", "Asalariado informal", true, "Más de 1", false);
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(conFormularioOficial));
        when(cliente.calcular(any())).thenThrow(new IaNoDisponibleException("caida", new RuntimeException()));

        service.precalificar(40L);

        ArgumentCaptor<SolicitudPuntaje> enviada = ArgumentCaptor.forClass(SolicitudPuntaje.class);
        verify(cliente).calcular(enviada.capture());
        assertThat(enviada.getValue().dependenciaEconomica()).isEqualTo("Independiente");
        assertThat(enviada.getValue().ocupacionSosten()).isEqualTo("Asalariado informal");
        assertThat(enviada.getValue().tieneHijos()).isTrue();
        assertThat(enviada.getValue().cantidadHijos()).isEqualTo("Más de 1");
        assertThat(enviada.getValue().discapacidad()).isFalse();
    }

    @Test
    void precalificar_unaPostulacionSinLosDatosDelFormularioOficial_losManda_nulos() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenThrow(new IaNoDisponibleException("caida", new RuntimeException()));

        service.precalificar(40L);

        ArgumentCaptor<SolicitudPuntaje> enviada = ArgumentCaptor.forClass(SolicitudPuntaje.class);
        verify(cliente).calcular(enviada.capture());
        assertThat(enviada.getValue().dependenciaEconomica()).isNull();
        assertThat(enviada.getValue().tieneHijos()).isNull();
        assertThat(enviada.getValue().discapacidad()).isNull();
    }

    @Test
    void precalificar_sinNadaPendiente_noLlamaALaIa() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.empty());

        assertThat(service.precalificar(40L)).isEqualTo(Resultado.OMITIDA);
        verify(cliente, never()).calcular(any());
        verify(calificacion, never()).marcarCalificada(anyLong());
    }

    @Test
    void precalificar_conLaIaCaida_dejaLaPostulacionPendiente() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenThrow(new IaNoDisponibleException("caida", new RuntimeException()));

        assertThat(service.precalificar(40L)).isEqualTo(Resultado.IA_NO_DISPONIBLE);
        verify(puntajes, never()).saveAndFlush(any());
        verify(calificacion, never()).marcarCalificada(anyLong());
    }

    @Test
    void precalificar_conDatosRechazadosPorLaIa_noCalculaNiCalifica() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenThrow(new IaDatosInvalidosException("422"));

        assertThat(service.precalificar(40L)).isEqualTo(Resultado.NO_CALCULADA);
        verify(calificacion, never()).marcarCalificada(anyLong());
        verify(consulta).registrarFallo(40L, PrecalificacionService.INTENTOS_MAXIMOS);
    }

    @Test
    void precalificar_conLaIaCaida_noCuentaComoIntentoFallido() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenThrow(new IaNoDisponibleException("caida", null));

        service.precalificar(40L);

        verify(consulta, never()).registrarFallo(anyLong(), anyInt());
    }

    @Test
    void precalificar_siOtraSolicitudYaLaCalifico_noGeneraUnSegundoPuntaje() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenReturn(new PuntajeCalculado(new BigDecimal("63.40"), "v1", null));
        when(consulta.sigueEnPendiente(40L)).thenReturn(false);

        assertThat(service.precalificar(40L)).isEqualTo(Resultado.OMITIDA);
        verify(puntajes, never()).saveAndFlush(any());
        verify(calificacion, never()).marcarCalificada(anyLong());
    }

    @Test
    void precalificar_conUnaVersionDeModeloNoRegistrada_noCalifica() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenReturn(new PuntajeCalculado(new BigDecimal("63.40"), "v9", null));
        when(consulta.sigueEnPendiente(40L)).thenReturn(true);
        when(modelos.findByVersion("v9")).thenReturn(Optional.empty());

        assertThat(service.precalificar(40L)).isEqualTo(Resultado.NO_CALCULADA);
        verify(consulta).registrarFallo(40L, PrecalificacionService.INTENTOS_MAXIMOS);
        verify(consulta, never()).retirarVigentes(anyLong());
        verify(calificacion, never()).marcarCalificada(anyLong());
    }

    @Test
    void precalificar_sinVersionVigente_mandaLaSolicitudSinModeloNiParametros() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(datos));
        when(modelosConsulta.vigente()).thenReturn(Optional.empty());
        when(cliente.calcular(any())).thenThrow(new IaNoDisponibleException("caida", new RuntimeException()));

        service.precalificar(40L);

        ArgumentCaptor<SolicitudPuntaje> enviada = ArgumentCaptor.forClass(SolicitudPuntaje.class);
        verify(cliente).calcular(enviada.capture());
        assertThat(enviada.getValue().modelo()).isNull();
        assertThat(enviada.getValue().parametros()).isNull();
    }

    @Test
    void precalificar_conUnaVersionVigenteSinParametros_mandaLaSolicitudSinModeloNiParametros() {
        when(consulta.datosPendiente(40L)).thenReturn(Optional.of(datos));
        when(modelosConsulta.vigente()).thenReturn(Optional.of(new ModeloFila(7L, UUID.randomUUID(), "[PRUEBA] v2",
                null, true, AHORA)));
        when(modelosConsulta.parametros(7L)).thenReturn(List.of());
        when(cliente.calcular(any())).thenThrow(new IaNoDisponibleException("caida", new RuntimeException()));

        service.precalificar(40L);

        ArgumentCaptor<SolicitudPuntaje> enviada = ArgumentCaptor.forClass(SolicitudPuntaje.class);
        verify(cliente).calcular(enviada.capture());
        assertThat(enviada.getValue().modelo()).isNull();
        assertThat(enviada.getValue().parametros()).isNull();
    }

    // ---- recalcular (CU26) ----

    private final ModeloAplicable v14 = new ModeloAplicable(9L, "v1.4",
            Map.of("dependientes", new BigDecimal("20.00")));

    @Test
    void recalcular_conLaIaDisponible_conservaElAnteriorRegistraElNuevoYNoCambiaElEstado() {
        when(consulta.datosCalificada(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenReturn(new PuntajeCalculado(new BigDecimal("55.10"), "v1.4", null));
        when(consulta.sigueCalificada(40L)).thenReturn(true);
        when(consulta.tieneVigenteDelModelo(40L, 9L)).thenReturn(false);
        when(entityManager.getReference(ModeloPreseleccion.class, 9L)).thenReturn(modelo);
        when(entityManager.getReference(Postulacion.class, 40L)).thenReturn(mock(Postulacion.class));

        Resultado resultado = service.recalcular(40L, v14);

        assertThat(resultado).isEqualTo(Resultado.RECALCULADA);
        ArgumentCaptor<SolicitudPuntaje> enviada = ArgumentCaptor.forClass(SolicitudPuntaje.class);
        verify(cliente).calcular(enviada.capture());
        assertThat(enviada.getValue().modelo()).isEqualTo("v1.4");
        assertThat(enviada.getValue().parametros()).containsEntry("dependientes", new BigDecimal("20.00"));
        ArgumentCaptor<PuntajePostulacion> guardado = ArgumentCaptor.forClass(PuntajePostulacion.class);
        verify(consulta).retirarVigentes(40L);
        verify(puntajes).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getPuntajeTotal()).isEqualByComparingTo("55.10");
        assertThat(guardado.getValue().getModelo()).isSameAs(modelo);
        assertThat(guardado.getValue().isVigente()).isTrue();
        assertThat(guardado.getValue().getFechaCalculo()).isEqualTo(AHORA);
        verify(calificacion, never()).marcarCalificada(anyLong());
    }

    @Test
    void recalcular_sinPostulacionCalificada_noLlamaALaIa() {
        when(consulta.datosCalificada(40L)).thenReturn(Optional.empty());

        assertThat(service.recalcular(40L, v14)).isEqualTo(Resultado.OMITIDA);
        verify(cliente, never()).calcular(any());
    }

    @Test
    void recalcular_conLaIaCaida_noTocaElPuntaje() {
        when(consulta.datosCalificada(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenThrow(new IaNoDisponibleException("caida", new RuntimeException()));

        assertThat(service.recalcular(40L, v14)).isEqualTo(Resultado.IA_NO_DISPONIBLE);
        verify(consulta, never()).retirarVigentes(anyLong());
        verify(puntajes, never()).saveAndFlush(any());
    }

    @Test
    void recalcular_conDatosRechazadosPorLaIa_quedaNoCalculada() {
        when(consulta.datosCalificada(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenThrow(new IaDatosInvalidosException("422"));

        assertThat(service.recalcular(40L, v14)).isEqualTo(Resultado.NO_CALCULADA);
        verify(puntajes, never()).saveAndFlush(any());
    }

    @Test
    void recalcular_siLaIaRespondeConOtraVersion_noSeRegistra() {
        when(consulta.datosCalificada(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenReturn(new PuntajeCalculado(new BigDecimal("55.10"), "v1.3", null));

        assertThat(service.recalcular(40L, v14)).isEqualTo(Resultado.NO_CALCULADA);
        verify(consulta, never()).retirarVigentes(anyLong());
        verify(puntajes, never()).saveAndFlush(any());
    }

    @Test
    void recalcular_siYaTieneElPuntajeDeEsaVersion_noGeneraOtro() {
        when(consulta.datosCalificada(40L)).thenReturn(Optional.of(datos));
        when(cliente.calcular(any())).thenReturn(new PuntajeCalculado(new BigDecimal("55.10"), "v1.4", null));
        when(consulta.sigueCalificada(40L)).thenReturn(true);
        when(consulta.tieneVigenteDelModelo(40L, 9L)).thenReturn(true);

        assertThat(service.recalcular(40L, v14)).isEqualTo(Resultado.OMITIDA);
        verify(consulta, never()).retirarVigentes(anyLong());
        verify(puntajes, never()).saveAndFlush(any());
    }

    @Test
    void solicitar_unFalloInesperado_noSePropaga() {
        when(consulta.datosPendiente(40L)).thenThrow(new IllegalStateException("base caida"));

        service.solicitar(40L); // no lanza: corre en segundo plano y el envio ya se confirmo

        verify(cliente, never()).calcular(any());
    }

    @Test
    void reintento_califcaLasPendientesYCortaCuandoLaIaNoResponde() {
        PrecalificacionService precalificacion = mock(PrecalificacionService.class);
        PrecalificacionConsultaRepository pendientes = mock(PrecalificacionConsultaRepository.class);
        when(pendientes.pendientes(PrecalificacionReintentoJob.LOTE)).thenReturn(List.of(1L, 2L, 3L, 4L));
        when(precalificacion.precalificar(1L)).thenReturn(Resultado.CALIFICADA);
        when(precalificacion.precalificar(2L)).thenReturn(Resultado.NO_CALCULADA);
        when(precalificacion.precalificar(3L)).thenReturn(Resultado.IA_NO_DISPONIBLE);

        int calificadas = new PrecalificacionReintentoJob(pendientes, precalificacion).reintentar();

        assertThat(calificadas).isEqualTo(1);
        verify(precalificacion, never()).precalificar(4L);
    }
}
