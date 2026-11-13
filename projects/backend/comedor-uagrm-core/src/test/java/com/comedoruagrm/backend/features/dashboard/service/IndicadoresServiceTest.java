package com.comedoruagrm.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.IndicadorMensual;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.ConvocatoriaDatos;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.FilaMensual;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.GestionDatos;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.TotalesFila;

/** La consulta de indicadores del dashboard (CU12) con el repositorio simulado. */
class IndicadoresServiceTest {

    private final IndicadoresRepository repositorio = mock(IndicadoresRepository.class);
    private final FiltrosDashboardService filtros = mock(FiltrosDashboardService.class);
    private final IndicadoresService servicio = new IndicadoresService(repositorio, filtros);

    private final GestionDatos gestion = new GestionDatos(7, UUID.randomUUID(), "2026-II");
    private final ConvocatoriaDatos convocatoria = new ConvocatoriaDatos(70, UUID.randomUUID(), "Beca 2026",
            gestion);

    private void conDatos() {
        when(repositorio.totales(anyLong(), any())).thenReturn(new TotalesFila(10, 4, 2));
        when(repositorio.serieMensual(anyLong(), any())).thenReturn(List.of(
                new FilaMensual(LocalDate.of(2026, 9, 1), 6, 0, 0),
                new FilaMensual(LocalDate.of(2026, 12, 1), 4, 4, 2)));
        when(repositorio.ultimaActualizacion()).thenReturn(Optional.of(Instant.parse("2026-11-13T06:00:00Z")));
    }

    @Test
    void sinFiltros_usaLaGestionVigente() {
        when(filtros.gestionVigente()).thenReturn(
                Optional.of(new FiltrosDashboardService.GestionVigente(7, gestion.codigo(), "2026-II")));
        conDatos();

        IndicadoresResponse r = servicio.consultar(null, null);

        assertThat(r.gestion().codigo()).isEqualTo(gestion.codigo());
        assertThat(r.convocatoria()).isNull();
        assertThat(r.totales().postulacionesRecibidas()).isEqualTo(10);
        assertThat(r.totales().finalistas()).isEqualTo(4);
        assertThat(r.totales().becasAdjudicadas()).isEqualTo(2);
        assertThat(r.actualizadoEn()).isEqualTo(Instant.parse("2026-11-13T06:00:00Z"));
        assertThat(r.sinDatos()).isFalse();
        verify(repositorio).totales(7, null);
    }

    @Test
    void conGestion_laResuelvePorSuCodigo() {
        when(repositorio.gestion(gestion.codigo())).thenReturn(Optional.of(gestion));
        conDatos();

        IndicadoresResponse r = servicio.consultar(gestion.codigo(), null);

        assertThat(r.gestion().etiqueta()).isEqualTo("2026-II");
        verify(repositorio).serieMensual(7, null);
        verify(filtros, never()).gestionVigente();
    }

    @Test
    void soloConConvocatoria_laGestionEsLaDeEsaConvocatoria() {
        when(repositorio.convocatoria(convocatoria.codigo())).thenReturn(Optional.of(convocatoria));
        conDatos();

        IndicadoresResponse r = servicio.consultar(null, convocatoria.codigo());

        assertThat(r.gestion().codigo()).isEqualTo(gestion.codigo());
        assertThat(r.convocatoria().codigo()).isEqualTo(convocatoria.codigo());
        assertThat(r.convocatoria().nombre()).isEqualTo("Beca 2026");
        verify(repositorio).totales(7, 70L);
    }

    @Test
    void gestionInexistente_lanzaErrDas01() {
        when(repositorio.gestion(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar(UUID.randomUUID(), null)).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo("ERR_DAS_01");
    }

    @Test
    void convocatoriaInexistente_lanzaErrDas01() {
        when(repositorio.convocatoria(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar(null, UUID.randomUUID())).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo("ERR_DAS_01");
    }

    @Test
    void convocatoriaQueNoEsDeLaGestion_lanzaErrDas01() {
        when(repositorio.convocatoria(convocatoria.codigo())).thenReturn(Optional.of(convocatoria));

        assertThatThrownBy(() -> servicio.consultar(UUID.randomUUID(), convocatoria.codigo()))
                .isInstanceOf(NotFoundException.class).extracting("errorCode").isEqualTo("ERR_DAS_01");
        verify(repositorio, never()).totales(anyLong(), any());
    }

    @Test
    void convocatoriaDeLaGestionIndicada_noFalla() {
        when(repositorio.convocatoria(convocatoria.codigo())).thenReturn(Optional.of(convocatoria));
        conDatos();

        assertThat(servicio.consultar(gestion.codigo(), convocatoria.codigo()).convocatoria()).isNotNull();
    }

    @Test
    void sinGestiones_respondeVacioSinError() {
        when(filtros.gestionVigente()).thenReturn(Optional.empty());

        IndicadoresResponse r = servicio.consultar(null, null);

        assertThat(r.gestion()).isNull();
        assertThat(r.sinDatos()).isTrue();
        assertThat(r.porMes()).isEmpty();
        assertThat(r.actualizadoEn()).isNull();
        assertThat(r.totales().postulacionesRecibidas()).isZero();
        verify(repositorio, never()).totales(anyLong(), any());
    }

    @Test
    void laSerie_seCompletaConCerosEntreElPrimeroYElUltimoMes() {
        when(repositorio.gestion(gestion.codigo())).thenReturn(Optional.of(gestion));
        conDatos();

        List<IndicadorMensual> serie = servicio.consultar(gestion.codigo(), null).porMes();

        assertThat(serie).extracting(IndicadorMensual::mes).containsExactly("2026-09", "2026-10", "2026-11", "2026-12");
        assertThat(serie).extracting(IndicadorMensual::postulaciones).containsExactly(6L, 0L, 0L, 4L);
        assertThat(serie).extracting(IndicadorMensual::adjudicadas).containsExactly(0L, 0L, 0L, 2L);
    }

    @Test
    void sinDatos_cuandoTodosLosTotalesSonCero_yActualizadoEnNuloSiElLoteNuncaCorrio() {
        when(repositorio.gestion(gestion.codigo())).thenReturn(Optional.of(gestion));
        when(repositorio.totales(anyLong(), any())).thenReturn(new TotalesFila(0, 0, 0));
        when(repositorio.serieMensual(anyLong(), any())).thenReturn(List.of());
        when(repositorio.ultimaActualizacion()).thenReturn(Optional.empty());

        IndicadoresResponse r = servicio.consultar(gestion.codigo(), null);

        assertThat(r.sinDatos()).isTrue();
        assertThat(r.actualizadoEn()).isNull();
        assertThat(r.porMes()).isEmpty();
        assertThat(r.gestion()).isNotNull();
    }

    @Test
    void laAsistencia_esNulaEnLosTotalesYEnCadaMes() {
        when(repositorio.gestion(gestion.codigo())).thenReturn(Optional.of(gestion));
        conDatos();

        IndicadoresResponse r = servicio.consultar(gestion.codigo(), null);

        assertThat(r.totales().asistenciaPromedio()).isNull();
        assertThat(r.porMes()).allMatch(m -> m.asistencia() == null);
    }

    @Test
    void consultar_exigeRolDirectivo_yLeeEnUnaTransaccionDeSoloLectura() throws NoSuchMethodException {
        Method m = IndicadoresService.class.getMethod("consultar", UUID.class, UUID.class);

        assertThat(m.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('DIRECTIVO')");
        assertThat(m.getAnnotation(Transactional.class).readOnly()).isTrue();
        // Sin bitacora: el dashboard solo lee agregados (CA09).
        assertThat(m.getAnnotations()).noneMatch(a -> a.annotationType().getSimpleName().equals("Auditar"));
        verify(repositorio, never()).convocatoria(isNull());
    }
}
