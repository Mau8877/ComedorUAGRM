package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.features.postulaciones.dto.DecisionAdjudicacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.NoAdjudicarRequest;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.AdjudicacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.Adjudicacion;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.CabeceraFinalista;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.UltimaAuditoria;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/**
 * Pruebas de caja blanca de CU-05 (capitulo 9, 9.3.2): un test por camino basico factible de
 * `AdjudicacionService.decidir` (C1 a C5), invocado por `adjudicar` y `noAdjudicar`. Los caminos
 * C6 a C8 no son factibles (los nodos 10, 12, 15 y 18 evaluan la misma variable).
 */
class AdjudicacionServiceCajaBlancaTest {

    private final AdjudicacionConsultaRepository consulta = mock(AdjudicacionConsultaRepository.class);
    private final AdjudicacionRepository adjudicaciones = mock(AdjudicacionRepository.class);
    private final PostulacionRepository postulaciones = mock(PostulacionRepository.class);
    private final PostulacionHistorialRepository historial = mock(PostulacionHistorialRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final AdjudicacionService service = new AdjudicacionService(consulta, adjudicaciones, postulaciones,
            historial, usuarios);

    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID finalistaCodigo = UUID.randomUUID();
    private final UUID postulacionCodigo = UUID.randomUUID();
    private final UUID convocatoriaCodigo = UUID.randomUUID();
    private final Usuario actor = usuario(7L);
    private final Postulacion postulacion = nuevaPostulacion();

    private static Usuario usuario(long id) {
        Usuario u = new Usuario(null, "1234" + id, "admin@uagrm.edu.bo", "hash", "Admin", "Perez");
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "codigo", UUID.randomUUID());
        return u;
    }

    private static Postulacion nuevaPostulacion() {
        try {
            var ctor = Postulacion.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Postulacion p = ctor.newInstance();
            ReflectionTestUtils.setField(p, "estado", EstadoPostulacion.calificada);
            return p;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private DatosConvocatoria convocatoria(String estado) {
        return new DatosConvocatoria(9L, convocatoriaCodigo, "Conv", estado, 5, new BigDecimal("60"), 10);
    }

    /** Escenario base: convocatoria cerrada con cupos y minimo, finalista calificado y auditado (validado). */
    private void escenario(String estadoConvocatoria, boolean publicados, String estadoPostulacion) {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(consulta.cabeceraFinalista(finalistaCodigo)).thenReturn(Optional.of(new CabeceraFinalista(1L,
                finalistaCodigo, 3L, postulacionCodigo, 9L, convocatoriaCodigo, "calificada", new BigDecimal("45"))));
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria(estadoConvocatoria)));
        when(consulta.resultadosPublicados(9L)).thenReturn(publicados);
        when(consulta.estadoActual(3L)).thenReturn(estadoPostulacion);
        when(consulta.ultimaAuditoria(1L))
                .thenReturn(Optional.of(new UltimaAuditoria("VALIDADO", new BigDecimal("20"))));
        when(consulta.contarAdjudicadas(9L)).thenReturn(2L);
        when(postulaciones.findById(3L)).thenReturn(Optional.of(postulacion));
        when(adjudicaciones.saveAndFlush(any(Adjudicacion.class))).thenAnswer(i -> {
            Adjudicacion a = i.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 60L);
            ReflectionTestUtils.setField(a, "codigo", UUID.randomUUID());
            return a;
        });
        when(historial.saveAndFlush(any(PostulacionHistorial.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void fallaConCodigo(String codigo) {
        assertThatThrownBy(() -> service.adjudicar(actorCodigo, finalistaCodigo))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(codigo));
        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.calificada);
    }

    @Test
    void decidir_C1_resultadosPublicados_lanzaErrAdj01() {
        escenario("CERRADA", true, "calificada");

        fallaConCodigo(AdjudicacionErrorCodes.ERR_ADJ_01);
    }

    @Test
    void decidir_C2_convocatoriaSinDecisionesPosibles_lanzaErrAdj05() {
        escenario("ABIERTA", false, "calificada");

        fallaConCodigo(AdjudicacionErrorCodes.ERR_ADJ_05);
    }

    @Test
    void decidir_C3_postulacionYaNoCalificada_lanzaErrAdj01() {
        escenario("CERRADA", false, "adjudicada");

        fallaConCodigo(AdjudicacionErrorCodes.ERR_ADJ_01);
    }

    @Test
    void decidir_C4_adjudicar_pasaAAdjudicadaYGuardaAdjudicacionEHistorial() {
        escenario("CERRADA", false, "calificada");

        ResultadoAuditado<DecisionAdjudicacionResponse> r = service.adjudicar(actorCodigo, finalistaCodigo);

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.adjudicada);
        ArgumentCaptor<PostulacionHistorial> fila = ArgumentCaptor.forClass(PostulacionHistorial.class);
        verify(historial).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getEstadoNuevo()).isEqualTo(EstadoPostulacion.adjudicada);
        verify(adjudicaciones).saveAndFlush(any(Adjudicacion.class));
        assertThat(r.datos().estado()).isEqualTo("adjudicada");
        assertThat(r.datos().cuposDisponibles()).isEqualTo(3);
    }

    @Test
    void decidir_C5_noAdjudicar_pasaANoAdjudicadaYRegistraElMotivo() {
        escenario("CERRADA", false, "calificada");

        ResultadoAuditado<DecisionAdjudicacionResponse> r = service.noAdjudicar(actorCodigo, finalistaCodigo,
                new NoAdjudicarRequest("No cumple el requisito"));

        assertThat(postulacion.getEstado()).isEqualTo(EstadoPostulacion.no_adjudicada);
        ArgumentCaptor<PostulacionHistorial> fila = ArgumentCaptor.forClass(PostulacionHistorial.class);
        verify(historial).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getEstadoNuevo()).isEqualTo(EstadoPostulacion.no_adjudicada);
        assertThat(fila.getValue().getMotivo()).isEqualTo("No cumple el requisito");
        assertThat(r.datos().estado()).isEqualTo("no_adjudicada");
    }
}
