package com.comedoruagrm.backend.features.becas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.becas.enums.EstadoBeca;
import com.comedoruagrm.backend.features.becas.enums.TipoMovimientoBeca;
import com.comedoruagrm.backend.features.becas.exception.BecaErrorCodes;
import com.comedoruagrm.backend.features.becas.model.Beca;
import com.comedoruagrm.backend.features.becas.model.MovimientoBeca;
import com.comedoruagrm.backend.features.becas.repository.BecaRepository;
import com.comedoruagrm.backend.features.becas.repository.MovimientoBecaRepository;
import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository;
import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository.CabeceraBeca;
import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository.Candidato;
import com.comedoruagrm.backend.features.becas.repository.PadronConsultaRepository.FilaBeca;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class BecaEstadoServiceTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID BECA = UUID.randomUUID();

    @Mock
    private PadronConsultaRepository consulta;

    @Mock
    private BecaRepository becaRepository;

    @Mock
    private MovimientoBecaRepository movimientoRepository;

    @Mock
    private UsuarioService usuarioService;

    private BecaEstadoService service;
    private Usuario actor;

    @BeforeEach
    void preparar() {
        // 10/11/2026 02:00 UTC son las 22:00 del 9/11 en La Paz.
        Clock reloj = Clock.fixed(Instant.parse("2026-11-10T02:00:00Z"), ZoneOffset.UTC);
        service = new BecaEstadoService(consulta, becaRepository, movimientoRepository, usuarioService, reloj);
        actor = mock(Usuario.class);
        // lenient: no todos los tests llegan a pedir el actor (algunos fallan antes o son de solo lectura).
        lenient().when(actor.getId()).thenReturn(7L);
        lenient().when(actor.getCodigo()).thenReturn(ACTOR);
        lenient().when(usuarioService.obtenerPorCodigo(ACTOR)).thenReturn(actor);
    }

    private Beca beca(EstadoBeca estado) {
        Beca beca = new Beca(3L, 4L, 5L, LocalDate.of(2026, 11, 1));
        ReflectionTestUtils.setField(beca, "id", 10L);
        ReflectionTestUtils.setField(beca, "codigo", BECA);
        if (estado == EstadoBeca.BAJA) {
            beca.darDeBaja();
        } else if (estado == EstadoBeca.SUSPENDIDA) {
            beca.suspender();
        }
        return beca;
    }

    private void becaBloqueada(EstadoBeca estado) {
        when(consulta.bloquearBeca(BECA)).thenReturn(Optional.of(new CabeceraBeca(10L, BECA, 3L, estado.name())));
    }

    private FilaBeca fila(String estado) {
        return new FilaBeca(BECA, UUID.randomUUID(), "Ana", "Rojas", "101", "Sistemas", estado,
                LocalDate.of(2026, 11, 1), null);
    }

    @Test
    void darDeBaja_conBecaVigente_cambiaElEstadoYGuardaElMovimientoConElMotivoRecortado() {
        becaBloqueada(EstadoBeca.VIGENTE);
        Beca beca = beca(EstadoBeca.VIGENTE);
        when(becaRepository.findById(10L)).thenReturn(Optional.of(beca));
        when(consulta.fila(10L)).thenReturn(Optional.of(fila("BAJA")));
        when(becaRepository.saveAndFlush(any(Beca.class))).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepository.saveAndFlush(any(MovimientoBeca.class))).thenAnswer(i -> {
            MovimientoBeca m = i.getArgument(0);
            ReflectionTestUtils.setField(m, "codigo", UUID.randomUUID());
            return m;
        });

        var resultado = service.darDeBaja(ACTOR, BECA, "  Dejó la universidad  ");

        assertThat(beca.getEstado()).isEqualTo(EstadoBeca.BAJA);
        ArgumentCaptor<MovimientoBeca> movimiento = ArgumentCaptor.forClass(MovimientoBeca.class);
        verify(movimientoRepository).saveAndFlush(movimiento.capture());
        assertThat(movimiento.getValue().getTipo()).isEqualTo(TipoMovimientoBeca.BAJA);
        assertThat(movimiento.getValue().getMotivo()).isEqualTo("Dejó la universidad");
        assertThat(movimiento.getValue().getRegistradoPor()).isEqualTo(7L);
        // La fecha del movimiento es la de La Paz, no la de UTC.
        assertThat(movimiento.getValue().getFecha()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.detalle()).containsEntry("estadoAnterior", "VIGENTE").containsEntry("estadoNuevo",
                "BAJA");
        assertThat(resultado.detalle().toString()).doesNotContain("universidad");
    }

    @Test
    void suspender_conBecaSuspendida_lanzaErrBec05SinTocarNada() {
        becaBloqueada(EstadoBeca.SUSPENDIDA);

        assertThatThrownBy(() -> service.suspender(ACTOR, BECA, "x")).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(BecaErrorCodes.ERR_BEC_05);
        verify(becaRepository, never()).saveAndFlush(any());
        verify(movimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void darDeBaja_conBecaInexistente_lanzaErrBec01() {
        when(consulta.bloquearBeca(BECA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.darDeBaja(ACTOR, BECA, "x")).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo(BecaErrorCodes.ERR_BEC_01);
    }

    @Test
    void reemplazar_conBecaVigente_lanzaErrBec06SinBuscarCandidato() {
        when(consulta.cabeceraBeca(BECA)).thenReturn(Optional.of(new CabeceraBeca(10L, BECA, 3L, "VIGENTE")));
        when(consulta.bloquearBeca(BECA)).thenReturn(Optional.of(new CabeceraBeca(10L, BECA, 3L, "VIGENTE")));

        assertThatThrownBy(() -> service.reemplazar(ACTOR, BECA, UUID.randomUUID()))
                .isInstanceOf(ConflictException.class).extracting("errorCode")
                .isEqualTo(BecaErrorCodes.ERR_BEC_06);
        verify(consulta, never()).siguienteDisponible(3L);
    }

    @Test
    void reemplazar_conUnCandidatoDistinto_lanzaErrBec08SinCrearLaBeca() {
        CabeceraBeca baja = new CabeceraBeca(10L, BECA, 3L, "BAJA");
        when(consulta.cabeceraBeca(BECA)).thenReturn(Optional.of(baja));
        when(consulta.bloquearBeca(BECA)).thenReturn(Optional.of(baja));
        when(consulta.yaReemplazada(10L)).thenReturn(false);
        when(consulta.siguienteDisponible(3L)).thenReturn(Optional.of(new Candidato(9L, UUID.randomUUID(), "Luis",
                "Mamani", "102", "Derecho", 3, new BigDecimal("75.00"))));

        assertThatThrownBy(() -> service.reemplazar(ACTOR, BECA, UUID.randomUUID()))
                .isInstanceOf(ConflictException.class).extracting("errorCode")
                .isEqualTo(BecaErrorCodes.ERR_BEC_08);
        verify(becaRepository, never()).saveAndFlush(any());
        verify(movimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void candidatoDeReemplazo_sinCandidato_lanzaErrBec07() {
        when(consulta.cabeceraBeca(BECA)).thenReturn(Optional.of(new CabeceraBeca(10L, BECA, 3L, "BAJA")));
        when(consulta.yaReemplazada(10L)).thenReturn(false);
        when(consulta.siguienteDisponible(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.candidatoDeReemplazo(BECA)).isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(BecaErrorCodes.ERR_BEC_07);
    }
}
