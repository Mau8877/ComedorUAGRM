package com.comedoruagrm.backend.features.resultados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionLoteService;
import com.comedoruagrm.backend.features.resultados.dto.PublicacionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.model.PublicacionResultado;
import com.comedoruagrm.backend.features.resultados.repository.PublicacionResultadoRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Conteos;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosPublicacion;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Destinatario;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/**
 * Pruebas de caja blanca de CU-22 (capitulo 9, 9.3.2): un test por camino basico de
 * `PublicacionResultadosService.publicar` (C1 a C4).
 */
class PublicacionResultadosServiceCajaBlancaTest {

    private static final long CONVOCATORIA_ID = 7L;

    private final ResultadosConsultaRepository consulta = mock(ResultadosConsultaRepository.class);
    private final PublicacionResultadoRepository publicacionRepository = mock(PublicacionResultadoRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final NotificacionLoteService notificacionLoteService = mock(NotificacionLoteService.class);
    private final PublicacionResultadosService service = new PublicacionResultadosService(consulta,
            publicacionRepository, usuarioService, notificacionLoteService);

    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();
    private Usuario admin;

    @BeforeEach
    void preparar() {
        admin = new Usuario(null, "0000001", "admin@dubss.com", "hash", "Admin", "DUBSS");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "codigo", actorCodigo);
    }

    private void convocatoria(String estado, boolean yaPublicada, Conteos conteos) {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        when(consulta.convocatoria(convocatoria)).thenReturn(
                Optional.of(new DatosConvocatoria(CONVOCATORIA_ID, convocatoria, "Beca 2026-II", estado)));
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(yaPublicada
                ? Optional.of(new DatosPublicacion(Instant.now(), "Admin DUBSS")) : Optional.empty());
        when(consulta.contarPorEstado(CONVOCATORIA_ID)).thenReturn(conteos);
    }

    private void fallaConCodigo(String codigo) {
        assertThatThrownBy(() -> service.publicar(actorCodigo, convocatoria)).isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(codigo));
        verify(publicacionRepository, never()).saveAndFlush(any());
        verify(consulta, never()).marcarConvocatoriaPublicada(CONVOCATORIA_ID);
    }

    @Test
    void publicar_C1_yaPublicada_lanzaErrPub02() {
        convocatoria("PUBLICADA", true, new Conteos(2, 1, 0, 3));

        fallaConCodigo("ERR_PUB_02");
    }

    @Test
    void publicar_C2_convocatoriaNoCerrada_lanzaErrPub03() {
        convocatoria("ABIERTA", false, new Conteos(0, 0, 0, 0));

        fallaConCodigo("ERR_PUB_03");
    }

    @Test
    void publicar_C3_conCalificadasSinDecidir_lanzaErrPub01() {
        convocatoria("CERRADA", false, new Conteos(2, 1, 3, 6));

        fallaConCodigo("ERR_PUB_01");
    }

    @Test
    void publicar_C4_todoDecidido_guardaMarcaPublicadaYAvisa() {
        convocatoria("CERRADA", false, new Conteos(1, 1, 0, 2));
        when(consulta.destinatariosDelAviso(CONVOCATORIA_ID)).thenReturn(List.of(
                new Destinatario(11, "Ana", "ana@prueba.test", 111, UUID.randomUUID(), "ADJUDICADA"),
                new Destinatario(12, "Beto", "beto@prueba.test", 112, UUID.randomUUID(), "NO_ADJUDICADA")));
        when(publicacionRepository.saveAndFlush(any(PublicacionResultado.class))).thenAnswer(i -> {
            PublicacionResultado p = i.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 50L);
            ReflectionTestUtils.setField(p, "codigo", UUID.randomUUID());
            return p;
        });
        when(notificacionLoteService.notificarLote(any())).thenAnswer(i -> ((List<?>) i.getArgument(0)).size());

        ResultadoAuditado<PublicacionResultadoResponse> r = service.publicar(actorCodigo, convocatoria);

        verify(publicacionRepository).saveAndFlush(any(PublicacionResultado.class));
        verify(consulta).marcarConvocatoriaPublicada(CONVOCATORIA_ID);
        assertThat(r.datos().convocatoria().estado()).isEqualTo("PUBLICADA");
        assertThat(r.datos().adjudicados()).isEqualTo(1);
        assertThat(r.datos().noAdjudicados()).isEqualTo(1);
        assertThat(r.datos().avisados()).isEqualTo(2);
    }
}
