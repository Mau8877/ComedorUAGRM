package com.comedoruagrm.backend.features.asistencias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.asistencias.dto.AsistenciaResponse;
import com.comedoruagrm.backend.features.asistencias.dto.BecadoAsistenciaResponse;
import com.comedoruagrm.backend.features.asistencias.exception.AsistenciasErrorCodes;
import com.comedoruagrm.backend.features.asistencias.model.Asistencia;
import com.comedoruagrm.backend.features.asistencias.repository.AsistenciaRepository;
import com.comedoruagrm.backend.features.asistencias.repository.AsistenciasConsultaRepository;
import com.comedoruagrm.backend.features.asistencias.repository.AsistenciasConsultaRepository.Becado;
import com.comedoruagrm.backend.features.asistencias.repository.AsistenciasConsultaRepository.CabeceraBeca;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Buscar al becado y registrar su asistencia (CU10) con los colaboradores simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsistenciaServiceTest {

    // 02:30 UTC del 17/11 es el 16/11 22:30 en La Paz: la fecha de hoy es la de Bolivia, no la de UTC.
    private static final Instant AHORA = Instant.parse("2026-11-17T02:30:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);
    private static final long BECA_ID = 40L;

    @Mock
    private AsistenciasConsultaRepository consulta;

    @Mock
    private AsistenciaRepository asistenciaRepository;

    @Mock
    private UsuarioService usuarioService;

    private AsistenciaService service;
    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID becaCodigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new AsistenciaService(consulta, asistenciaRepository, usuarioService,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        Usuario personal = new Usuario(null, "0000002", "comedor@dubss.com", "hash", "Pedro", "Comedor");
        ReflectionTestUtils.setField(personal, "id", 2L);
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(personal);
    }

    private void beca(String estado) {
        when(consulta.cabeceraBeca(becaCodigo)).thenReturn(Optional.of(new CabeceraBeca(BECA_ID, becaCodigo, estado)));
    }

    private Asistencia asistencia(Instant hora) {
        Asistencia a = BeanUtils.instantiateClass(Asistencia.class);
        ReflectionTestUtils.setField(a, "codigo", UUID.fromString("00000000-0000-7000-8000-000000000077"));
        ReflectionTestUtils.setField(a, "horaRegistro", hora);
        return a;
    }

    // ---- buscarBecado ----

    @Test
    void buscarBecado_conCiDeUnBecado_devuelveSusDatosYLaFechaDeHoyEnBolivia() {
        when(consulta.buscarPorCi("9812241", HOY)).thenReturn(Optional.of(new Becado(BECA_ID, becaCodigo, "Ana",
                "Rojas Vaca", "9812241", "Ing. Sistemas", "Beca 2026-II", "VIGENTE", null)));

        BecadoAsistenciaResponse r = service.buscarBecado("  9812241 ");

        assertThat(r.becaCodigo()).isEqualTo(becaCodigo);
        assertThat(r.nombres()).isEqualTo("Ana");
        assertThat(r.estadoBeca()).isEqualTo("VIGENTE");
        assertThat(r.fecha()).isEqualTo(HOY);
        assertThat(r.horaRegistro()).isNull();
    }

    @Test
    void buscarBecado_conCiQueYaRegistroHoy_devuelveLaHoraDelRegistro() {
        Instant hora = Instant.parse("2026-11-16T17:00:00Z");
        when(consulta.buscarPorCi("9812241", HOY)).thenReturn(Optional.of(new Becado(BECA_ID, becaCodigo, "Ana",
                "Rojas Vaca", "9812241", "Ing. Sistemas", "Beca 2026-II", "VIGENTE", hora)));

        assertThat(service.buscarBecado("9812241").horaRegistro()).isEqualTo(hora);
    }

    @Test
    void buscarBecado_conCiSinBeca_lanzaNotFoundErrAsi01() {
        when(consulta.buscarPorCi(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarBecado("1"))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AsistenciasErrorCodes.ERR_ASI_01))
                .hasMessageNotContaining("1 ");
    }

    // ---- registrar ----

    @Test
    void registrar_conBecaVigenteSinAsistenciaHoy_laCreaConLaFechaDeBolivia() {
        beca("VIGENTE");
        when(asistenciaRepository.insertarSiNoExiste(eq(BECA_ID), eq(2L), eq(HOY), eq(AHORA))).thenReturn(1);
        when(asistenciaRepository.findByBecaIdAndFecha(BECA_ID, HOY)).thenReturn(Optional.of(asistencia(AHORA)));

        AsistenciaResponse r = service.registrar(actorCodigo, becaCodigo);

        assertThat(r.yaRegistrada()).isFalse();
        assertThat(r.fecha()).isEqualTo(HOY);
        assertThat(r.becaCodigo()).isEqualTo(becaCodigo);
        assertThat(r.horaRegistro()).isEqualTo(AHORA);
    }

    @Test
    void registrar_conAsistenciaDeHoyExistente_noCreaOtraYDevuelveLaHoraDelPrimero() {
        Instant primera = Instant.parse("2026-11-16T16:00:00Z");
        beca("VIGENTE");
        when(asistenciaRepository.insertarSiNoExiste(anyLong(), anyLong(), any(), any())).thenReturn(0);
        when(asistenciaRepository.findByBecaIdAndFecha(BECA_ID, HOY)).thenReturn(Optional.of(asistencia(primera)));

        AsistenciaResponse r = service.registrar(actorCodigo, becaCodigo);

        assertThat(r.yaRegistrada()).isTrue();
        assertThat(r.horaRegistro()).isEqualTo(primera);
    }

    @Test
    void registrar_conBecaSuspendidaODeBaja_lanzaConflictErrAsi02YNoInserta() {
        for (String estado : new String[] {"SUSPENDIDA", "BAJA"}) {
            beca(estado);

            assertThatThrownBy(() -> service.registrar(actorCodigo, becaCodigo))
                    .isInstanceOfSatisfying(ConflictException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(AsistenciasErrorCodes.ERR_ASI_02));
        }
        verify(asistenciaRepository, never()).insertarSiNoExiste(anyLong(), anyLong(), any(), any());
    }

    @Test
    void registrar_conBecaInexistente_lanzaNotFoundErrAsi03() {
        when(consulta.cabeceraBeca(becaCodigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(actorCodigo, becaCodigo))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AsistenciasErrorCodes.ERR_ASI_03));
        verify(asistenciaRepository, never()).insertarSiNoExiste(anyLong(), anyLong(), any(), any());
    }

    // ---- seguridad y transaccion ----

    @Test
    void losMetodosPublicos_exigenElRolPersonalComedor_yRegistrarEsTransaccional() throws NoSuchMethodException {
        var buscar = AsistenciaService.class.getMethod("buscarBecado", String.class);
        var registrar = AsistenciaService.class.getMethod("registrar", UUID.class, UUID.class);

        assertThat(buscar.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('PERSONAL_COMEDOR')");
        assertThat(registrar.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('PERSONAL_COMEDOR')");
        assertThat(registrar.getAnnotation(Transactional.class)).isNotNull();
        assertThat(buscar.getAnnotation(Transactional.class).readOnly()).isTrue();
    }
}
