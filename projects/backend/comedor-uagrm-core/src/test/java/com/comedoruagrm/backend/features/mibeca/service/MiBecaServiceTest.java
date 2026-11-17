package com.comedoruagrm.backend.features.mibeca.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.mibeca.dto.AsistenciaBecadoResponse;
import com.comedoruagrm.backend.features.mibeca.dto.MiBecaResponse;
import com.comedoruagrm.backend.features.mibeca.exception.MiBecaErrorCodes;
import com.comedoruagrm.backend.features.mibeca.repository.MiBecaConsultaRepository;
import com.comedoruagrm.backend.features.mibeca.repository.MiBecaConsultaRepository.Beca;
import com.comedoruagrm.backend.features.mibeca.service.MiBecaService.PaginaAsistencias;

/** Consulta del becado sobre su beca y su historial (CU24) con el repository simulado. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MiBecaServiceTest {

    private static final long BECA_ID = 40L;

    @Mock
    private MiBecaConsultaRepository consulta;

    private MiBecaService service;
    private final UUID usuario = UUID.randomUUID();
    private final UUID becaCodigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new MiBecaService(consulta);
    }

    private void beca(String estado, String motivo) {
        when(consulta.buscarBeca(usuario)).thenReturn(Optional.of(new Beca(BECA_ID, becaCodigo, estado,
                "Convocatoria 2026-II", "2026-II", Instant.parse("2026-08-28T14:00:00Z"),
                LocalDate.of(2026, 8, 28), null, motivo, 13)));
    }

    @Test
    void miBeca_conBeca_devuelveEstadoYResumen() {
        beca("VIGENTE", null);

        MiBecaResponse respuesta = service.miBeca(usuario);

        assertThat(respuesta.codigo()).isEqualTo(becaCodigo);
        assertThat(respuesta.estado()).isEqualTo("VIGENTE");
        assertThat(respuesta.convocatoria()).isEqualTo("Convocatoria 2026-II");
        assertThat(respuesta.gestion()).isEqualTo("2026-II");
        assertThat(respuesta.motivo()).isNull();
        assertThat(respuesta.totalAsistencias()).isEqualTo(13);
    }

    @Test
    void miBeca_conBecaDeBaja_devuelveElMotivo() {
        beca("BAJA", "Abandonó la universidad");

        MiBecaResponse respuesta = service.miBeca(usuario);

        assertThat(respuesta.estado()).isEqualTo("BAJA");
        assertThat(respuesta.motivo()).isEqualTo("Abandonó la universidad");
    }

    @Test
    void miBeca_sinBeca_lanzaNotFoundConErrMbe01() {
        when(consulta.buscarBeca(usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.miBeca(usuario)).isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((NotFoundException) e).getErrorCode())
                        .isEqualTo(MiBecaErrorCodes.ERR_MBE_01));
    }

    @Test
    void asistencias_sinBeca_lanzaNotFoundConErrMbe01() {
        when(consulta.buscarBeca(usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asistencias(usuario, 1, 20, null, null, null))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((NotFoundException) e).getErrorCode())
                        .isEqualTo(MiBecaErrorCodes.ERR_MBE_01));
        verify(consulta, never()).listarAsistencias(anyLong(), any(), any(), anyBoolean(), anyInt(), anyLong());
    }

    @Test
    void asistencias_porDefecto_ordenaDeLaMasRecienteYCalculaMeta() {
        beca("VIGENTE", null);
        LocalDate desde = LocalDate.of(2026, 9, 1);
        LocalDate hasta = LocalDate.of(2026, 9, 30);
        when(consulta.contarAsistencias(BECA_ID, desde, hasta)).thenReturn(45L);
        List<AsistenciaBecadoResponse> pagina = List.of(
                new AsistenciaBecadoResponse(LocalDate.of(2026, 9, 18), Instant.parse("2026-09-18T16:00:00Z")));
        when(consulta.listarAsistencias(BECA_ID, desde, hasta, true, 20, 20L)).thenReturn(pagina);

        PaginaAsistencias resultado = service.asistencias(usuario, 2, 20, desde, hasta, null);

        assertThat(resultado.datos()).isEqualTo(pagina);
        assertThat(resultado.meta().page()).isEqualTo(2);
        assertThat(resultado.meta().pageSize()).isEqualTo(20);
        assertThat(resultado.meta().totalItems()).isEqualTo(45);
        assertThat(resultado.meta().totalPages()).isEqualTo(3);
    }

    @Test
    void asistencias_conSortFecha_ordenaAscendente() {
        beca("VIGENTE", null);

        service.asistencias(usuario, 1, 10, null, null, "fecha");

        verify(consulta).listarAsistencias(eq(BECA_ID), eq(null), eq(null), eq(false), eq(10), eq(0L));
    }

    @Test
    void asistencias_sinRegistros_devuelvePaginaVaciaConMeta() {
        beca("VIGENTE", null);
        when(consulta.contarAsistencias(BECA_ID, null, null)).thenReturn(0L);
        when(consulta.listarAsistencias(BECA_ID, null, null, true, 20, 0L)).thenReturn(List.of());

        PaginaAsistencias resultado = service.asistencias(usuario, 1, 20, null, null, "-fecha");

        assertThat(resultado.datos()).isEmpty();
        assertThat(resultado.meta().totalItems()).isZero();
        assertThat(resultado.meta().totalPages()).isZero();
    }

    @Test
    void asistencias_conDesdePosteriorAHasta_lanzaValidationConErrMbe02() {
        assertThatThrownBy(() -> service.asistencias(usuario, 1, 20, LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 9, 1), null)).isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getErrorCode())
                        .isEqualTo(MiBecaErrorCodes.ERR_MBE_02));
    }

    @Test
    void asistencias_conSortNoSoportado_lanzaValidationConErrMbe03() {
        assertThatThrownBy(() -> service.asistencias(usuario, 1, 20, null, null, "-horaRegistro"))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getErrorCode())
                        .isEqualTo(MiBecaErrorCodes.ERR_MBE_03));
    }

    @Test
    void asistencias_conPaginaInvalida_lanzaValidationConErrSys01() {
        assertThatThrownBy(() -> service.asistencias(usuario, 0, 20, null, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getErrorCode()).isEqualTo("ERR_SYS_01"));
        assertThatThrownBy(() -> service.asistencias(usuario, 1, 0, null, null, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void metodos_exigenRolPostulanteYSonDeSoloLectura() throws NoSuchMethodException {
        var miBeca = MiBecaService.class.getMethod("miBeca", UUID.class);
        var asistencias = MiBecaService.class.getMethod("asistencias", UUID.class, int.class, int.class,
                LocalDate.class, LocalDate.class, String.class);

        for (var metodo : List.of(miBeca, asistencias)) {
            assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('POSTULANTE')");
            assertThat(metodo.getAnnotation(Transactional.class).readOnly()).isTrue();
        }
    }
}
