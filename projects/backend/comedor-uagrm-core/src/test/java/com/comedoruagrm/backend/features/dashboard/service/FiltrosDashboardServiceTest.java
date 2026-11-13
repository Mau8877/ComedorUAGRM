package com.comedoruagrm.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.dashboard.dto.FiltrosDashboardResponse;
import com.comedoruagrm.backend.features.dashboard.dto.FiltrosDashboardResponse.GestionFiltro;
import com.comedoruagrm.backend.features.dashboard.repository.FiltrosDashboardRepository;
import com.comedoruagrm.backend.features.dashboard.repository.FiltrosDashboardRepository.ConvocatoriaFila;
import com.comedoruagrm.backend.features.dashboard.repository.FiltrosDashboardRepository.GestionFila;

/** El catalogo de filtros y la gestion por defecto del dashboard (CU12), con el repositorio simulado. */
class FiltrosDashboardServiceTest {

    private final FiltrosDashboardRepository repositorio = mock(FiltrosDashboardRepository.class);

    private FiltrosDashboardService conHoy(String instanteIso) {
        return new FiltrosDashboardService(repositorio,
                Clock.fixed(Instant.parse(instanteIso), ZoneOffset.UTC));
    }

    private static GestionFila gestion(long id, String etiqueta, String inicio, String fin) {
        return new GestionFila(id, UUID.randomUUID(), etiqueta, LocalDate.parse(inicio), LocalDate.parse(fin));
    }

    @Test
    void obtener_marcaComoVigenteLaGestionQueContieneHoy() {
        GestionFila nueva = gestion(2, "2026-II", "2026-07-01", "2026-12-31");
        GestionFila vieja = gestion(1, "2026-I", "2026-01-01", "2026-06-30");
        when(repositorio.gestiones()).thenReturn(List.of(nueva, vieja));
        when(repositorio.convocatorias()).thenReturn(List.of());

        FiltrosDashboardResponse r = conHoy("2026-03-10T12:00:00Z").obtener();

        assertThat(r.gestiones()).extracting(GestionFiltro::etiqueta).containsExactly("2026-II", "2026-I");
        assertThat(r.gestiones()).extracting(GestionFiltro::vigente).containsExactly(false, true);
    }

    @Test
    void obtener_usaLaFechaDeLaPaz_noLaDeUtc() {
        GestionFila antes = gestion(1, "2026-I", "2026-01-01", "2026-06-30");
        GestionFila despues = gestion(2, "2026-II", "2026-07-01", "2026-12-31");
        when(repositorio.gestiones()).thenReturn(List.of(despues, antes));
        when(repositorio.convocatorias()).thenReturn(List.of());

        // 01:00 UTC del 1 de julio todavia es 30 de junio en La Paz (UTC-4).
        FiltrosDashboardResponse r = conHoy("2026-07-01T01:00:00Z").obtener();

        assertThat(r.gestiones()).extracting(GestionFiltro::vigente).containsExactly(false, true);
    }

    @Test
    void obtener_sinGestionVigente_tomaLaDeInicioMasReciente() {
        GestionFila reciente = gestion(2, "2026-II", "2026-07-01", "2026-12-31");
        GestionFila vieja = gestion(1, "2026-I", "2026-01-01", "2026-06-30");
        when(repositorio.gestiones()).thenReturn(List.of(reciente, vieja));
        when(repositorio.convocatorias()).thenReturn(List.of());

        FiltrosDashboardResponse r = conHoy("2027-05-01T12:00:00Z").obtener();

        assertThat(r.gestiones()).extracting(GestionFiltro::vigente).containsExactly(true, false);
    }

    @Test
    void obtener_sinGestiones_devuelveListaVacia() {
        when(repositorio.gestiones()).thenReturn(List.of());
        when(repositorio.convocatorias()).thenReturn(List.of());

        FiltrosDashboardService servicio = conHoy("2026-03-10T12:00:00Z");

        assertThat(servicio.obtener().gestiones()).isEmpty();
        assertThat(servicio.gestionVigente()).isEmpty();
    }

    @Test
    void obtener_agrupaLasConvocatoriasBajoSuGestion() {
        GestionFila g2 = gestion(2, "2026-II", "2026-07-01", "2026-12-31");
        GestionFila g1 = gestion(1, "2026-I", "2026-01-01", "2026-06-30");
        when(repositorio.gestiones()).thenReturn(List.of(g2, g1));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        when(repositorio.convocatorias()).thenReturn(List.of(new ConvocatoriaFila(2, a, "Beca A"),
                new ConvocatoriaFila(1, b, "Beca B"), new ConvocatoriaFila(2, c, "Beca C")));

        FiltrosDashboardResponse r = conHoy("2026-08-01T12:00:00Z").obtener();

        assertThat(r.gestiones().get(0).convocatorias()).extracting(x -> x.codigo()).containsExactly(a, c);
        assertThat(r.gestiones().get(1).convocatorias()).extracting(x -> x.codigo()).containsExactly(b);
    }

    @Test
    void gestionVigente_devuelveLaVigente() {
        GestionFila g = gestion(2, "2026-II", "2026-07-01", "2026-12-31");
        when(repositorio.gestiones()).thenReturn(List.of(g, gestion(1, "2026-I", "2026-01-01", "2026-06-30")));

        FiltrosDashboardService.GestionVigente vigente = conHoy("2026-08-01T12:00:00Z").gestionVigente().orElseThrow();

        assertThat(vigente.id()).isEqualTo(2);
        assertThat(vigente.codigo()).isEqualTo(g.codigo());
        assertThat(vigente.etiqueta()).isEqualTo("2026-II");
    }

    @Test
    void losMetodosPublicos_exigenRolDirectivo_yLeenEnUnaTransaccionDeSoloLectura() throws NoSuchMethodException {
        for (Method m : List.of(FiltrosDashboardService.class.getMethod("obtener"),
                FiltrosDashboardService.class.getMethod("gestionVigente"))) {
            assertThat(m.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('DIRECTIVO')");
            assertThat(m.getAnnotation(Transactional.class).readOnly()).isTrue();
        }
    }
}
