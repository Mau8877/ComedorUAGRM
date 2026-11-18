package com.comedoruagrm.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.features.dashboard.dto.BiResponse;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaAsistenciaBi;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaCarrera;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaConvocatoria;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaDiaSemana;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaGestion;
import com.comedoruagrm.backend.features.dashboard.repository.BiRepository.FilaTramo;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.ConvocatoriaDatos;
import com.comedoruagrm.backend.features.dashboard.repository.IndicadoresRepository.GestionDatos;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresService.Seleccion;

/** El analisis BI del dashboard (CU12) con los repositorios simulados: formulas, orden y casos sin datos. */
class BiServiceTest {

    private final BiRepository repositorio = mock(BiRepository.class);
    private final IndicadoresRepository indicadoresRepositorio = mock(IndicadoresRepository.class);
    private final IndicadoresService indicadores = mock(IndicadoresService.class);
    private final BiService servicio = new BiService(repositorio, indicadoresRepositorio, indicadores);

    private final GestionDatos gestion = new GestionDatos(7, UUID.randomUUID(), "2026-II");
    private final UUID codigoUno = UUID.randomUUID();
    private final UUID codigoDos = UUID.randomUUID();

    private static FilaConvocatoria fila(long id, UUID codigo, String nombre, Integer cupos, long recibidas,
            long precalificadas, Double puntaje, long finalistas, long adjudicadas, long vigentes, long riesgo) {
        return new FilaConvocatoria(id, codigo, nombre, cupos, recibidas, precalificadas, puntaje, finalistas,
                adjudicadas, vigentes, 1, 2, 3, riesgo);
    }

    private void conDatos() {
        when(indicadores.resolver(any(), any())).thenReturn(new Seleccion(gestion, null));
        when(repositorio.convocatorias(7)).thenReturn(List.of(
                fila(10, codigoUno, "Beca I", 20, 100, 80, 40.0, 30, 15, 12, 1),
                fila(20, codigoDos, "Beca II", 30, 150, 120, 50.0, 40, 20, 25, 3)));
        when(repositorio.carreras(anyLong(), any())).thenReturn(List.of(
                new FilaCarrera("Tecnología", "Informática", 120, 20),
                new FilaCarrera("Tecnología", "Redes", 30, 5),
                new FilaCarrera("Humanidades", "Derecho", 100, 10)));
        when(repositorio.tramosPuntaje(anyLong(), any())).thenReturn(List.of(new FilaTramo(2, 5), new FilaTramo(6, 9)));
        when(repositorio.asistenciaTotal(anyLong(), any())).thenReturn(new FilaAsistenciaBi(75, 100, 12));
        when(repositorio.asistenciaPorDiaSemana(anyLong(), any())).thenReturn(List.of(new FilaDiaSemana(1, 9, 10),
                new FilaDiaSemana(5, 1, 4)));
        when(repositorio.gestiones()).thenReturn(List.of(
                new FilaGestion(UUID.randomUUID(), "2025-II", 200, 10),
                new FilaGestion(gestion.codigo(), "2026-II", 250, 35)));
        when(indicadoresRepositorio.ultimaActualizacion()).thenReturn(Optional.of(Instant.parse("2026-11-13T06:00:00Z")));
    }

    @Test
    void elEmbudoSumaLasConvocatoriasDeLaGestion() {
        conDatos();

        BiResponse r = servicio.consultar(null, null);

        assertThat(r.embudo().postulaciones()).isEqualTo(250);
        assertThat(r.embudo().precalificadas()).isEqualTo(200);
        assertThat(r.embudo().finalistas()).isEqualTo(70);
        assertThat(r.embudo().adjudicadas()).isEqualTo(35);
        assertThat(r.embudo().becasVigentes()).isEqualTo(37);
        assertThat(r.sinDatos()).isFalse();
        assertThat(r.actualizadoEn()).isEqualTo(Instant.parse("2026-11-13T06:00:00Z"));
        assertThat(r.gestion().etiqueta()).isEqualTo("2026-II");
    }

    @Test
    void conUnaConvocatoria_elEmbudoLaRespeta_peroElComparativoSigueCompleto() {
        conDatos();
        when(indicadores.resolver(any(), any())).thenReturn(new Seleccion(gestion,
                new ConvocatoriaDatos(20, codigoDos, "Beca II", gestion)));

        BiResponse r = servicio.consultar(null, codigoDos);

        assertThat(r.embudo().postulaciones()).isEqualTo(150);
        assertThat(r.convocatoria().codigo()).isEqualTo(codigoDos);
        assertThat(r.convocatorias()).hasSize(2);
        assertThat(r.convocatorias()).extracting(c -> c.seleccionada()).containsExactly(false, true);
        verify(repositorio).carreras(7, 20L);
    }

    @Test
    void lasFacultades_vanDeLaQueMasPostulaAlaQueMenos_conSuParteDelTotalYLaTasa() {
        conDatos();

        BiResponse r = servicio.consultar(null, null);

        assertThat(r.facultades()).extracting(f -> f.nombre()).containsExactly("Tecnología", "Humanidades");
        var tecnologia = r.facultades().get(0);
        assertThat(tecnologia.postulaciones()).isEqualTo(150);
        assertThat(tecnologia.adjudicadas()).isEqualTo(25);
        assertThat(tecnologia.porcentajePostulaciones()).isEqualTo(60.0);
        assertThat(tecnologia.tasaAdjudicacion()).isEqualTo(16.7);
        assertThat(tecnologia.carreras()).extracting(c -> c.nombre()).containsExactly("Informática", "Redes");
        assertThat(r.facultades().get(1).porcentajePostulaciones()).isEqualTo(40.0);
    }

    @Test
    void elComparativo_calculaLaTasaYLaVariacionContraLaAnterior() {
        conDatos();

        BiResponse r = servicio.consultar(null, null);

        var primera = r.convocatorias().get(0);
        var segunda = r.convocatorias().get(1);
        assertThat(primera.variacionPostulaciones()).isNull();
        assertThat(primera.tasaAdjudicacion()).isEqualTo(15.0);
        assertThat(segunda.variacionPostulaciones()).isEqualTo(50.0);
        assertThat(segunda.tasaAdjudicacion()).isEqualTo(13.3);
        assertThat(segunda.puntajePromedio()).isEqualTo(50.0);
        assertThat(segunda.cupos()).isEqualTo(30);
    }

    @Test
    void elPuntaje_tieneSiempreSieteTramos_yElPromedioPesaPorPrecalificadas() {
        conDatos();

        BiResponse r = servicio.consultar(null, null);

        assertThat(r.puntaje()).hasSize(7);
        assertThat(r.puntaje().get(2).cantidad()).isEqualTo(5);
        assertThat(r.puntaje().get(2).desde()).isEqualTo(20);
        assertThat(r.puntaje().get(2).hasta()).isEqualTo(30);
        assertThat(r.puntaje().get(0).cantidad()).isZero();
        assertThat(r.puntaje().get(6).cantidad()).isEqualTo(9);
        // (40 x 80 + 50 x 120) / 200 = 46
        assertThat(r.puntajePromedio()).isEqualTo(46.0);
    }

    @Test
    void laCobertura_comparaLasBecasVigentesConLosCupos() {
        conDatos();

        BiResponse r = servicio.consultar(null, null);

        assertThat(r.cobertura().cupos()).isEqualTo(50);
        assertThat(r.cobertura().becasVigentes()).isEqualTo(37);
        assertThat(r.cobertura().becasSuspendidas()).isEqualTo(2);
        assertThat(r.cobertura().becasBaja()).isEqualTo(4);
        assertThat(r.cobertura().reemplazos()).isEqualTo(6);
        assertThat(r.cobertura().porcentaje()).isEqualTo(74.0);
    }

    @Test
    void laAsistencia_traeLaTasa_losDiasConRegistro_lasBecasEnRiesgo_yLosSieteDias() {
        conDatos();

        BiResponse r = servicio.consultar(null, null);

        assertThat(r.asistencia().tasaPromedio()).isEqualTo(75.0);
        assertThat(r.asistencia().diasConRegistro()).isEqualTo(12);
        assertThat(r.asistencia().becasEnRiesgo()).isEqualTo(4);
        assertThat(r.asistencia().porDiaSemana()).hasSize(7);
        assertThat(r.asistencia().porDiaSemana().get(0).tasa()).isEqualTo(90.0);
        assertThat(r.asistencia().porDiaSemana().get(4).tasa()).isEqualTo(25.0);
        // Sin registro ese dia no hay tasa (no se inventa 0).
        assertThat(r.asistencia().porDiaSemana().get(1).tasa()).isNull();
    }

    @Test
    void laTendencia_marcaLaGestionSeleccionada_yVariaContraLaAnterior() {
        conDatos();

        BiResponse r = servicio.consultar(null, null);

        assertThat(r.tendencia()).extracting(t -> t.etiqueta()).containsExactly("2025-II", "2026-II");
        assertThat(r.tendencia().get(0).variacionPostulaciones()).isNull();
        assertThat(r.tendencia().get(1).variacionPostulaciones()).isEqualTo(25.0);
        assertThat(r.tendencia().get(0).seleccionada()).isFalse();
        assertThat(r.tendencia().get(1).seleccionada()).isTrue();
    }

    @Test
    void sinNadaQueMedir_losPorcentajesSonNull_noCero() {
        when(indicadores.resolver(any(), any())).thenReturn(new Seleccion(gestion, null));
        when(repositorio.convocatorias(7)).thenReturn(List.of(fila(10, codigoUno, "Beca I", null, 0, 0, null, 0, 0, 0, 0)));
        when(repositorio.asistenciaTotal(anyLong(), any())).thenReturn(new FilaAsistenciaBi(0, 0, 0));
        when(indicadoresRepositorio.ultimaActualizacion()).thenReturn(Optional.empty());

        BiResponse r = servicio.consultar(null, null);

        assertThat(r.sinDatos()).isTrue();
        assertThat(r.actualizadoEn()).isNull();
        assertThat(r.puntajePromedio()).isNull();
        assertThat(r.cobertura().porcentaje()).isNull();
        assertThat(r.asistencia().tasaPromedio()).isNull();
        assertThat(r.convocatorias().get(0).tasaAdjudicacion()).isNull();
        assertThat(r.facultades()).isEmpty();
        assertThat(r.puntaje()).hasSize(7).allMatch(t -> t.cantidad() == 0);
    }

    @Test
    void sinGestiones_laRespuestaVieneVacia_sinError() {
        when(indicadores.resolver(any(), any())).thenReturn(new Seleccion(null, null));

        BiResponse r = servicio.consultar(null, null);

        assertThat(r.gestion()).isNull();
        assertThat(r.sinDatos()).isTrue();
        assertThat(r.puntaje()).hasSize(7);
        assertThat(r.asistencia().porDiaSemana()).hasSize(7);
        assertThat(r.convocatorias()).isEmpty();
        assertThat(r.tendencia()).isEmpty();
    }

    @Test
    void variacion_yPorcentaje_noDividenPorCero() {
        assertThat(BiService.variacion(5, 0)).isNull();
        assertThat(BiService.variacion(50, 100)).isEqualTo(-50.0);
        assertThat(BiService.porcentaje(1, 0)).isNull();
        assertThat(BiService.porcentaje(1, 3)).isEqualTo(33.3);
    }

    @Test
    void consultar_exigeRolDirectivo_yLeeEnUnaTransaccionDeSoloLectura() throws NoSuchMethodException {
        Method m = BiService.class.getMethod("consultar", UUID.class, UUID.class);

        assertThat(m.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('DIRECTIVO')");
        assertThat(m.getAnnotation(Transactional.class).readOnly()).isTrue();
        // Sin bitacora: son agregados sin datos personales (CA09).
        assertThat(m.getAnnotations()).noneMatch(a -> a.annotationType().getSimpleName().equals("Auditar"));
    }
}
