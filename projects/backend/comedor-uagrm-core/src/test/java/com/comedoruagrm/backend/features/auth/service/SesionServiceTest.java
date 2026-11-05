package com.comedoruagrm.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.model.Sesion;
import com.comedoruagrm.backend.features.auth.repository.SesionRepository;
import com.comedoruagrm.backend.features.auth.service.SesionService.SesionAbierta;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.TokenService;
import com.comedoruagrm.backend.security.TokenService.TokenAcceso;

class SesionServiceTest {

    private final SesionRepository sesionRepository = mock(SesionRepository.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final SesionService service = new SesionService(sesionRepository, tokenService, 7);

    private final Usuario usuario = mock(Usuario.class);
    private final UUID codigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        when(usuario.getCodigo()).thenReturn(codigo);
        when(usuario.isActivo()).thenReturn(true);
        when(tokenService.emitirAcceso(codigo)).thenReturn(new TokenAcceso("jwt", 900));
    }

    @Test
    void abrir_guardaSoloElHashDelRefreshTokenYLoDevuelveEnClaro() {
        SesionAbierta abierta = service.abrir(usuario);

        ArgumentCaptor<Sesion> captor = ArgumentCaptor.forClass(Sesion.class);
        verify(sesionRepository).save(captor.capture());
        assertThat(abierta.refreshToken()).isNotBlank();
        assertThat(captor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(abierta.refreshToken());
        assertThat(captor.getValue().getExpiraEn()).isAfter(Instant.now().plusSeconds(6 * 24 * 3600));
        assertThat(abierta.acceso().token()).isEqualTo("jwt");
    }

    @Test
    void revocarTodas_pideAlRepositorioRevocarLasSesionesDeEsaCuenta() {
        when(usuario.getId()).thenReturn(9L);
        when(sesionRepository.revocarTodasDe(eq(9L), any(Instant.class))).thenReturn(3);

        service.revocarTodas(usuario);

        verify(sesionRepository).revocarTodasDe(eq(9L), any(Instant.class));
    }

    @Test
    void revocarTodas_sinSesionesVigentes_noEsUnError() {
        when(usuario.getId()).thenReturn(9L);
        when(sesionRepository.revocarTodasDe(eq(9L), any(Instant.class))).thenReturn(0);

        service.revocarTodas(usuario);

        verify(sesionRepository).revocarTodasDe(eq(9L), any(Instant.class));
    }

    @Test
    void revocarOtras_conElRefreshTokenActual_conservaEsaSesionPorSuHash() {
        when(usuario.getId()).thenReturn(9L);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        when(sesionRepository.revocarTodasMenos(eq(9L), hash.capture(), any(Instant.class))).thenReturn(2);

        service.revocarOtras(usuario, "refresh-actual");

        assertThat(hash.getValue()).hasSize(64).isNotEqualTo("refresh-actual");
        verify(sesionRepository, never()).revocarTodasDe(any(), any());
    }

    @Test
    void revocarOtras_sinRefreshToken_revocaTodasLasSesiones() {
        when(usuario.getId()).thenReturn(9L);

        service.revocarOtras(usuario, null);
        service.revocarOtras(usuario, "  ");

        verify(sesionRepository, times(2)).revocarTodasDe(eq(9L), any(Instant.class));
        verify(sesionRepository, never()).revocarTodasMenos(any(), any(), any());
    }

    @Test
    void abrir_dosVeces_emiteRefreshTokensDistintos() {
        assertThat(service.abrir(usuario).refreshToken()).isNotEqualTo(service.abrir(usuario).refreshToken());
    }

    @Test
    void renovar_conUnaSesionVigente_larevocaYAbreUnaNueva() {
        Sesion vigente = new Sesion(usuario, "hash", Instant.now().plusSeconds(3600));
        when(sesionRepository.findByTokenHash(any())).thenReturn(Optional.of(vigente));

        SesionAbierta nueva = service.renovar("token");

        assertThat(vigente.getRevocadaEn()).isNotNull();
        assertThat(nueva.refreshToken()).isNotEqualTo("token");
        verify(sesionRepository).save(any(Sesion.class));
    }

    @Test
    void renovar_conUnaSesionVencida_lanzaErrAuth06() {
        Sesion vencida = new Sesion(usuario, "hash", Instant.now().minusSeconds(1));
        when(sesionRepository.findByTokenHash(any())).thenReturn(Optional.of(vencida));

        assertThatThrownBy(() -> service.renovar("token"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_06);
    }

    @Test
    void renovar_conUnaSesionYaRevocada_lanzaErrAuth06() {
        Sesion revocada = new Sesion(usuario, "hash", Instant.now().plusSeconds(3600));
        revocada.setRevocadaEn(Instant.now());
        when(sesionRepository.findByTokenHash(any())).thenReturn(Optional.of(revocada));

        assertThatThrownBy(() -> service.renovar("token")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void renovar_conUnTokenDesconocido_lanzaErrAuth06() {
        when(sesionRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renovar("token")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void renovar_conLaCuentaDesactivada_lanzaForbiddenErrAuth03() {
        when(usuario.isActivo()).thenReturn(false);
        Sesion vigente = new Sesion(usuario, "hash", Instant.now().plusSeconds(3600));
        when(sesionRepository.findByTokenHash(any())).thenReturn(Optional.of(vigente));

        assertThatThrownBy(() -> service.renovar("token"))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AuthErrorCodes.ERR_AUTH_03);
    }

    @Test
    void cerrar_conUnaSesionVigente_larevoca() {
        Sesion vigente = new Sesion(usuario, "hash", Instant.now().plusSeconds(3600));
        when(sesionRepository.findByTokenHash(any())).thenReturn(Optional.of(vigente));

        service.cerrar("token");

        assertThat(vigente.getRevocadaEn()).isNotNull();
    }

    @Test
    void cerrar_conUnTokenDesconocido_noHaceNada() {
        when(sesionRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        service.cerrar("token");
    }
}
