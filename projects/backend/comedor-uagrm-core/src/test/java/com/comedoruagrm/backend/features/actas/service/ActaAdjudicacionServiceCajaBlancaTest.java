package com.comedoruagrm.backend.features.actas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.actas.exception.ActasErrorCodes;
import com.comedoruagrm.backend.features.actas.repository.ActaAdjudicacionRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/**
 * Pruebas de caja blanca de CU-08 (capitulo 9, 9.3.2): un test por camino basico de
 * `ActaAdjudicacionService.validar` (C1 a C3), metodo privado invocado por reflexion.
 */
class ActaAdjudicacionServiceCajaBlancaTest {

    private static final long CONVOCATORIA_ID = 7L;
    private static final Instant AHORA = Instant.parse("2026-11-13T15:30:00Z");

    private final ActasConsultaRepository consulta = mock(ActasConsultaRepository.class);
    private final ActaAdjudicacionRepository actaRepository = mock(ActaAdjudicacionRepository.class);
    private final ActaAdjudicacionService service = new ActaAdjudicacionService(consulta, actaRepository,
            mock(NumeroActaService.class), mock(ActaPdfService.class), mock(AlmacenamientoService.class),
            mock(UsuarioService.class), Clock.fixed(AHORA, ZoneOffset.UTC));

    private final DatosConvocatoria convocatoria = new DatosConvocatoria(CONVOCATORIA_ID, UUID.randomUUID(),
            "Beca 2026-II", "PUBLICADA", "2026-II", Instant.parse("2026-07-01T12:00:00Z"),
            Instant.parse("2026-08-31T12:00:00Z"), 50, new BigDecimal("60.00"));

    private void validar() {
        ReflectionTestUtils.invokeMethod(service, "validar", convocatoria);
    }

    @Test
    void validar_C1_yaTieneActa_lanzaErrAct02() {
        when(actaRepository.existsByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(true);

        assertThatThrownBy(this::validar).isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ActasErrorCodes.ERR_ACT_02));
    }

    @Test
    void validar_C2_sinPublicacionDeResultados_lanzaErrAct01() {
        when(actaRepository.existsByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(false);
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(this::validar).isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ActasErrorCodes.ERR_ACT_01));
    }

    @Test
    void validar_C3_sinActaYConPublicacion_terminaSinError() {
        when(actaRepository.existsByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(false);
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.of(Instant.parse("2026-11-12T12:00:00Z")));

        assertThatCode(this::validar).doesNotThrowAnyException();
    }
}
