package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.MiPostulacionResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.FilaListado;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.Pagina;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** El listado «Mis postulaciones» (CU04) con los colaboradores simulados. */
@ExtendWith(MockitoExtension.class)
class MisPostulacionesServiceTest {

    private static final long USUARIO_ID = 9L;
    private static final Instant ENVIO = Instant.parse("2026-11-01T15:00:00Z");

    @Mock
    private MisPostulacionesConsultaRepository consulta;

    @Mock
    private UsuarioService usuarioService;

    private MisPostulacionesService service;
    private final UUID usuarioCodigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new MisPostulacionesService(consulta, usuarioService);
        Usuario postulante = new Usuario(null, "1111111", "p@prueba.test", "hash", "Pia", "Perez");
        ReflectionTestUtils.setField(postulante, "id", USUARIO_ID);
        ReflectionTestUtils.setField(postulante, "codigo", usuarioCodigo);
        lenient().when(usuarioService.obtenerPorCodigo(usuarioCodigo)).thenReturn(postulante);
    }

    private static FilaListado fila(String estado, boolean publicada) {
        // La decision final (CU05) existe para adjudicada y no adjudicada; sin ella un «no adjudicada» es un rechazo.
        return fila(estado, publicada, estado.equals("adjudicada") || estado.equals("no_adjudicada"));
    }

    private static FilaListado fila(String estado, boolean publicada, boolean decisionFinal) {
        return new FilaListado(UUID.randomUUID(), UUID.randomUUID(), "Beca 2026-II", UUID.randomUUID(), "2026-II",
                ENVIO, estado, (short) 1, publicada, decisionFinal);
    }

    @Test
    void listar_mapeaLaFilaAlResumenConElEstadoVisible() {
        FilaListado f = fila("pendiente", false);
        when(consulta.listar(eq(USUARIO_ID), eq(null), eq(false), any(Pageable.class)))
                .thenReturn(new Pagina<>(List.of(f), 1));

        Page<MiPostulacionResumenResponse> pagina = service.listar(usuarioCodigo, null, PageRequest.of(0, 20));

        MiPostulacionResumenResponse r = pagina.getContent().get(0);
        assertThat(r.codigo()).isEqualTo(f.codigo());
        assertThat(r.convocatoria().codigo()).isEqualTo(f.convocatoriaCodigo());
        assertThat(r.convocatoria().nombre()).isEqualTo("Beca 2026-II");
        assertThat(r.gestionAcademica().codigo()).isEqualTo(f.gestionCodigo());
        assertThat(r.gestionAcademica().nombre()).isEqualTo("2026-II");
        assertThat(r.fechaEnvio()).isEqualTo(ENVIO);
        assertThat(r.estado()).isEqualTo("pendiente");
        assertThat(r.numeroEnvios()).isEqualTo((short) 1);
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listar_unaAdjudicadaSinPublicarSeVeCalificada_yPublicadaSeVeAdjudicada() {
        when(consulta.listar(anyLong(), any(), anyBoolean(), any(Pageable.class))).thenReturn(new Pagina<>(
                List.of(fila("adjudicada", false), fila("no_adjudicada", false), fila("adjudicada", true),
                        fila("no_adjudicada", true), fila("devuelta", false), fila("archivada", false)), 6));

        List<String> estados = service.listar(usuarioCodigo, null, PageRequest.of(0, 20)).getContent().stream()
                .map(MiPostulacionResumenResponse::estado).toList();

        assertThat(estados).containsExactly("calificada", "calificada", "adjudicada", "no_adjudicada", "devuelta",
                "archivada");
    }

    @Test
    void listar_unRechazoDelCu20_seVeNoAdjudicadaDeInmediato_sinPublicar() {
        when(consulta.listar(anyLong(), any(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new Pagina<>(List.of(fila("no_adjudicada", false, false)), 1));

        String estado = service.listar(usuarioCodigo, null, PageRequest.of(0, 20)).getContent().get(0).estado();

        assertThat(estado).isEqualTo("no_adjudicada");
    }

    @Test
    void listar_sinOrdenEsDescendente_yConSortFechaEnvioRespetaElSentido() {
        when(consulta.listar(anyLong(), any(), anyBoolean(), any(Pageable.class))).thenReturn(new Pagina<>(List.of(), 0));

        service.listar(usuarioCodigo, null, PageRequest.of(0, 20));
        service.listar(usuarioCodigo, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "fechaEnvio")));
        service.listar(usuarioCodigo, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fechaEnvio")));

        ArgumentCaptor<Boolean> ascendente = ArgumentCaptor.forClass(Boolean.class);
        verify(consulta, org.mockito.Mockito.times(3)).listar(anyLong(), any(), ascendente.capture(), any(Pageable.class));
        assertThat(ascendente.getAllValues()).containsExactly(false, true, false);
    }

    @Test
    void listar_conSortNoSoportado_lanzaErrSys05() {
        assertThatThrownBy(() -> service.listar(usuarioCodigo, null, PageRequest.of(0, 20, Sort.by("estado"))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_05"));
        assertThatThrownBy(() -> service.listar(usuarioCodigo, null,
                PageRequest.of(0, 20, Sort.by("fechaEnvio").and(Sort.by("estado")))))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_05"));
    }

    @Test
    void listar_filtraPorGestion_yUnCodigoMalFormadoEsErrSys05() {
        UUID gestion = UUID.randomUUID();
        when(consulta.listar(anyLong(), eq(gestion), anyBoolean(), any(Pageable.class)))
                .thenReturn(new Pagina<>(List.of(), 0));

        service.listar(usuarioCodigo, " " + gestion + " ", PageRequest.of(0, 20));

        verify(consulta).listar(eq(USUARIO_ID), eq(gestion), eq(false), any(Pageable.class));
        assertThatThrownBy(() -> service.listar(usuarioCodigo, "no-es-un-uuid", PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_05"));
    }

    @Test
    void listar_usaElIdDelUsuarioDelToken_yEsDeSoloLectura() throws NoSuchMethodException {
        when(consulta.listar(anyLong(), any(), anyBoolean(), any(Pageable.class))).thenReturn(new Pagina<>(List.of(), 0));

        service.listar(usuarioCodigo, null, PageRequest.of(0, 20));

        verify(consulta).listar(eq(USUARIO_ID), eq(null), eq(false), any(Pageable.class));
        Method metodo = MisPostulacionesService.class.getMethod("listar", UUID.class, String.class, Pageable.class);
        assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('POSTULANTE')");
        assertThat(metodo.getAnnotation(Transactional.class).readOnly()).isTrue();
    }
}
