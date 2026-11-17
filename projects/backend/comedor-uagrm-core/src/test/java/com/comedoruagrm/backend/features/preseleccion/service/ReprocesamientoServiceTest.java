package com.comedoruagrm.backend.features.preseleccion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.preseleccion.dto.LoteReprocesamientoResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.ResumenReprocesamientoResponse;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository.ModeloFila;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository.ConvocatoriaDatos;
import com.comedoruagrm.backend.features.preseleccion.repository.ReprocesamientoConsultaRepository.LoteFila;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

class ReprocesamientoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T15:00:00Z");

    private final ReprocesamientoConsultaRepository consulta = mock(ReprocesamientoConsultaRepository.class);
    private final ModeloPreseleccionConsultaRepository modelos = mock(ModeloPreseleccionConsultaRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final ReprocesamientoService service = new ReprocesamientoService(consulta, modelos, usuarios, eventos);

    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID convocatoriaCodigo = UUID.randomUUID();
    private final Usuario actor = mock(Usuario.class);
    private final ConvocatoriaDatos convocatoria = new ConvocatoriaDatos(4L, convocatoriaCodigo, "Convocatoria 2026-II",
            "2026-II");
    private final ModeloFila vigente = new ModeloFila(9L, UUID.randomUUID(), "v1.4", null, true, AHORA);

    private LoteFila lote(String estado, int total, int procesadas, int fallidas) {
        return new LoteFila(21L, UUID.randomUUID(), 4L, convocatoriaCodigo, 9L, "v1.4", estado, total, procesadas,
                fallidas, null, null);
    }

    private static String codigoDe(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    private void escenarioListo() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(actor.getId()).thenReturn(3L);
        when(actor.getCodigo()).thenReturn(actorCodigo);
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria));
        when(consulta.resultadosPublicados(4L)).thenReturn(false);
        when(modelos.vigente()).thenReturn(Optional.of(vigente));
        when(consulta.loteEnCurso(4L)).thenReturn(Optional.empty());
        when(consulta.porReprocesar(4L, 9L)).thenReturn(List.of(1L, 2L, 3L));
    }

    @Test
    void reprocesar_creaElLotePendientePublicaElEventoYAuditaConElTotal() {
        escenarioListo();
        when(consulta.crearLote(4L, 9L, 3L, 3)).thenReturn(lote("PENDIENTE", 3, 0, 0));

        ResultadoAuditado<LoteReprocesamientoResponse> resultado = service.reprocesar(actorCodigo,
                convocatoriaCodigo);

        assertThat(resultado.datos().estado()).isEqualTo("PENDIENTE");
        assertThat(resultado.datos().total()).isEqualTo(3);
        assertThat(resultado.datos().modelo()).isEqualTo("v1.4");
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.entidadId()).isEqualTo(21L);
        assertThat(resultado.detalle()).containsEntry("modelo", "v1.4").containsEntry("total", 3);
        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue()).isEqualTo(new LoteReprocesamientoCreado(21L));
    }

    @Test
    void reprocesar_conConvocatoriaInexistente_lanzaErrCv01() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reprocesar(actorCodigo, convocatoriaCodigo))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("ERR_CV_01"));
    }

    @Test
    void reprocesar_conResultadosPublicados_loBloqueaConErrMod04() {
        escenarioListo();
        when(consulta.resultadosPublicados(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.reprocesar(actorCodigo, convocatoriaCodigo))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("ERR_MOD_04"));
        verify(consulta, never()).crearLote(anyLong(), anyLong(), anyLong(), anyInt());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void reprocesar_sinVersionVigente_lanzaErrMod02() {
        escenarioListo();
        when(modelos.vigente()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reprocesar(actorCodigo, convocatoriaCodigo))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("ERR_MOD_02"));
    }

    @Test
    void reprocesar_conUnLoteEnCurso_lanzaErrMod03() {
        escenarioListo();
        when(consulta.loteEnCurso(4L)).thenReturn(Optional.of(lote("EN_PROCESO", 10, 4, 0)));

        assertThatThrownBy(() -> service.reprocesar(actorCodigo, convocatoriaCodigo))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("ERR_MOD_03"));
        verify(consulta, never()).crearLote(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void reprocesar_siDosPedidosLleganJuntos_elIndiceUnicoDaErrMod03() {
        escenarioListo();
        when(consulta.crearLote(4L, 9L, 3L, 3)).thenThrow(new DuplicateKeyException("uq lote en curso"));

        assertThatThrownBy(() -> service.reprocesar(actorCodigo, convocatoriaCodigo))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("ERR_MOD_03"));
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void reprocesar_sinNadaPorRecalcular_lanzaErrMod06() {
        escenarioListo();
        when(consulta.porReprocesar(4L, 9L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.reprocesar(actorCodigo, convocatoriaCodigo))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("ERR_MOD_06"));
    }

    // ---- resumen ----

    @Test
    void resumen_devuelveLaVersionVigenteLasCalificadasYLasQueFaltan() {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria));
        when(modelos.vigente()).thenReturn(Optional.of(vigente));
        when(consulta.calificadas(4L)).thenReturn(120);
        when(consulta.porReprocesar(4L, 9L)).thenReturn(List.of(1L, 2L));
        when(consulta.resultadosPublicados(4L)).thenReturn(false);
        when(consulta.loteEnCurso(4L)).thenReturn(Optional.empty());

        ResumenReprocesamientoResponse resumen = service.resumen(convocatoriaCodigo);

        assertThat(resumen.modelo()).isEqualTo("v1.4");
        assertThat(resumen.calificadas()).isEqualTo(120);
        assertThat(resumen.porReprocesar()).isEqualTo(2);
        assertThat(resumen.resultadosPublicados()).isFalse();
        assertThat(resumen.loteEnCurso()).isNull();
        assertThat(resumen.nombre()).isEqualTo("Convocatoria 2026-II");
        assertThat(resumen.gestion()).isEqualTo("2026-II");
    }

    @Test
    void resumen_sinVersionVigente_noCuentaNadaPorReprocesarYMuestraElLoteEnCurso() {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria));
        when(modelos.vigente()).thenReturn(Optional.empty());
        when(consulta.calificadas(4L)).thenReturn(5);
        when(consulta.loteEnCurso(4L)).thenReturn(Optional.of(lote("EN_PROCESO", 10, 4, 1)));

        ResumenReprocesamientoResponse resumen = service.resumen(convocatoriaCodigo);

        assertThat(resumen.modelo()).isNull();
        assertThat(resumen.porReprocesar()).isZero();
        assertThat(resumen.loteEnCurso().procesadas()).isEqualTo(4);
        assertThat(resumen.loteEnCurso().fallidas()).isEqualTo(1);
        verify(consulta, never()).porReprocesar(anyLong(), anyLong());
    }

    // ---- lote ----

    @Test
    void lote_devuelveElAvance() {
        LoteFila fila = lote("EN_PROCESO", 10, 4, 1);
        when(consulta.lote(fila.codigo())).thenReturn(Optional.of(fila));

        LoteReprocesamientoResponse respuesta = service.lote(fila.codigo());

        assertThat(respuesta.codigo()).isEqualTo(fila.codigo());
        assertThat(respuesta.convocatoria()).isEqualTo(convocatoriaCodigo);
        assertThat(respuesta.estado()).isEqualTo("EN_PROCESO");
        assertThat(respuesta.total()).isEqualTo(10);
    }

    @Test
    void lote_inexistente_lanzaErrMod05() {
        UUID codigo = UUID.randomUUID();
        when(consulta.lote(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lote(codigo)).isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("ERR_MOD_05"));
    }
}
