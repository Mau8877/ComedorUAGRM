package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.exception.ValidacionPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.ValidacionPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.ValidacionPostulacionConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.repository.ValidacionPostulacionConsultaRepository.DatosPostulante;
import com.comedoruagrm.backend.features.postulaciones.repository.ValidacionPostulacionConsultaRepository.PostulacionExistente;

class ValidacionPostulacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-07-16T15:00:00Z");

    private final ValidacionPostulacionConsultaRepository consulta = mock(ValidacionPostulacionConsultaRepository.class);
    private final ValidacionPostulacionService service = new ValidacionPostulacionService(consulta,
            Clock.fixed(AHORA, ZoneOffset.UTC));

    private final UUID usuario = UUID.randomUUID();
    private final UUID convocatoriaCodigo = UUID.randomUUID();
    private final DatosPostulante postulante = new DatosPostulante(8L, true);
    private final DatosConvocatoria abierta = new DatosConvocatoria(5L, 2L, "2026-II", "ABIERTA",
            AHORA.minusSeconds(86400), AHORA.plusSeconds(86400));

    private void dadoQueExiste(DatosConvocatoria convocatoria, DatosPostulante datosPostulante,
            PostulacionExistente... existentes) {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.of(convocatoria));
        when(consulta.postulanteDe(usuario)).thenReturn(Optional.ofNullable(datosPostulante));
        when(consulta.postulacionesEnLaGestion(8L, convocatoria.gestionId())).thenReturn(List.of(existentes));
    }

    private static PostulacionExistente existente(long convocatoriaId, String estado, UUID codigo) {
        return new PostulacionExistente(1L, codigo, convocatoriaId, estado, Instant.parse("2026-07-15T14:00:00Z"));
    }

    private static String codigoDe(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    @Test
    void validar_sinPostulacionesPrevias_devuelveValida() {
        dadoQueExiste(abierta, postulante);

        ResultadoValidacion r = service.validar(usuario, convocatoriaCodigo);

        assertThat(r.postulanteId()).isEqualTo(8L);
        assertThat(r.convocatoriaId()).isEqualTo(5L);
        assertThat(r.postulacionAReenviar()).isNull();
    }

    @Test
    void validar_conPostulacionVigenteEnLaMismaConvocatoria_lanzaDuplicada() {
        dadoQueExiste(abierta, postulante, existente(5L, "pendiente", UUID.randomUUID()));

        assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_01))
                .hasMessageContaining("2026-II").hasMessageContaining("pendiente").hasMessageContaining("15/07/2026");
    }

    @Test
    void validar_conPostulacionVigenteEnOtraConvocatoriaDeLaGestion_lanzaDuplicada() {
        dadoQueExiste(abierta, postulante, existente(4L, "archivada", UUID.randomUUID()));

        assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_01));
    }

    @Test
    void validar_conDecisionFinalSinPublicar_noRevelaElResultado() {
        dadoQueExiste(abierta, postulante, existente(4L, "adjudicada", UUID.randomUUID()));

        assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                .hasMessageContaining("calificada").hasMessageNotContaining("adjudicada");
    }

    @Test
    void validar_conBorradorDeLaMismaConvocatoria_noEsDuplicada() {
        dadoQueExiste(abierta, postulante, existente(5L, "borrador", UUID.randomUUID()));

        assertThat(service.validar(usuario, convocatoriaCodigo).postulacionAReenviar()).isNull();
    }

    @Test
    void validar_conBorradorDeOtraConvocatoria_noEsDuplicada() {
        dadoQueExiste(abierta, postulante, existente(4L, "borrador", UUID.randomUUID()));

        assertThat(service.validar(usuario, convocatoriaCodigo).postulacionAReenviar()).isNull();
    }

    @Test
    void validar_conDevueltaDeLaMismaConvocatoria_esReenvioYNoDuplicada() {
        UUID devuelta = UUID.randomUUID();
        dadoQueExiste(abierta, postulante, existente(5L, "devuelta", devuelta));

        assertThat(service.validar(usuario, convocatoriaCodigo).postulacionAReenviar()).isEqualTo(devuelta);
    }

    @Test
    void validar_conDevueltaDeOtraConvocatoriaDeLaGestion_esDuplicada() {
        dadoQueExiste(abierta, postulante, existente(4L, "devuelta", UUID.randomUUID()));

        assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_01));
    }

    @Test
    void validar_conDevueltaYOtraVigenteEnLaGestion_lanzaDuplicada() {
        dadoQueExiste(abierta, postulante, existente(5L, "devuelta", UUID.randomUUID()),
                existente(4L, "pendiente", UUID.randomUUID()));

        assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_01));
    }

    @Test
    void validar_conCuentaDesactivada_lanzaNoElegible() {
        dadoQueExiste(abierta, new DatosPostulante(8L, false));

        assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_02));
    }

    @Test
    void validar_conCuentaQueNoEsDePostulante_lanzaNoElegible() {
        dadoQueExiste(abierta, null);

        assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_02));
    }

    @Test
    void validar_conConvocatoriaEnBorradorOCerrada_lanzaNoRecibePostulaciones() {
        for (String estado : List.of("BORRADOR", "CERRADA", "PUBLICADA")) {
            dadoQueExiste(new DatosConvocatoria(5L, 2L, "2026-II", estado, AHORA.minusSeconds(86400),
                    AHORA.plusSeconds(86400)), postulante);

            assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                    .isInstanceOf(ConflictException.class)
                    .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_03));
        }
    }

    @Test
    void validar_conConvocatoriaAbiertaPeroFueraDeFechas_lanzaNoRecibePostulaciones() {
        DatosConvocatoria noEmpezo = new DatosConvocatoria(5L, 2L, "2026-II", "ABIERTA", AHORA.plusSeconds(60),
                AHORA.plusSeconds(86400));
        DatosConvocatoria yaPaso = new DatosConvocatoria(5L, 2L, "2026-II", "ABIERTA", AHORA.minusSeconds(86400),
                AHORA.minusSeconds(60));

        for (DatosConvocatoria c : List.of(noEmpezo, yaPaso)) {
            dadoQueExiste(c, postulante);

            assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                    .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_03));
        }
    }

    @Test
    void validar_conConvocatoriaInexistente_lanzaNotFound() {
        when(consulta.convocatoria(convocatoriaCodigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validar(usuario, convocatoriaCodigo))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ConvocatoriaErrorCodes.ERR_CV_01));
        verify(consulta, never()).postulanteDe(any());
    }

    @Test
    void validar_repetida_daElMismoResultado() {
        UUID devuelta = UUID.randomUUID();
        dadoQueExiste(abierta, postulante, existente(5L, "devuelta", devuelta));

        assertThat(service.validar(usuario, convocatoriaCodigo)).isEqualTo(service.validar(usuario, convocatoriaCodigo));
    }
}
