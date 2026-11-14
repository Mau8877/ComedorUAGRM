package com.comedoruagrm.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.CarreraRepository;
import com.comedoruagrm.backend.features.usuarios.repository.PostulanteRepository;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;
import com.comedoruagrm.backend.security.AuthErrorCodes;

class RestablecimientoContrasenaServiceTest {

    private static final String CORREO = "ana@uagrm.edu.bo";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SesionService sesionService = mock(SesionService.class);
    private final CorreoRestablecimientoService correoService = mock(CorreoRestablecimientoService.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final TokenEnlace tokenEnlace = new TokenEnlace();
    private final UsuarioService usuarioService = new UsuarioService(usuarioRepository,
            mock(PostulanteRepository.class), mock(RolRepository.class), mock(CarreraRepository.class),
            passwordEncoder);
    private final RestablecimientoContrasenaService service = new RestablecimientoContrasenaService(
            usuarioRepository, usuarioService, sesionService, tokenEnlace, correoService, 60);

    private Usuario ana;

    @BeforeEach
    void preparar() {
        Rol rol = mock(Rol.class);
        ana = new Usuario(rol, "1234567", CORREO, passwordEncoder.encode("clave-vieja"), "Ana", "Perez");
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(ana));
    }

    // ---- solicitar ----

    @Test
    void solicitar_conUnaCuentaActiva_guardaElHashNoElTokenYEnviaElTokenPorCorreo() {
        service.solicitar(CORREO);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarEnlace(eq(CORREO), eq("Ana"), token.capture());
        assertThat(ana.getRestablecimientoTokenHash()).isEqualTo(tokenEnlace.hash(token.getValue()))
                .isNotEqualTo(token.getValue());
        assertThat(ana.getRestablecimientoExpiraEn())
                .isBetween(Instant.now().plus(Duration.ofMinutes(59)), Instant.now().plus(Duration.ofMinutes(61)));
        verify(usuarioRepository).save(ana);
    }

    @Test
    void solicitar_conElCorreoEnMayusculasYEspacios_encuentraLaCuenta() {
        service.solicitar("  ANA@uagrm.edu.BO ");

        verify(correoService).enviarEnlace(eq(CORREO), any(), any());
    }

    @Test
    void solicitar_conUnCorreoDesconocido_noGuardaNiEnviaNada() {
        when(usuarioRepository.findByCorreo("nadie@uagrm.edu.bo")).thenReturn(Optional.empty());

        service.solicitar("nadie@uagrm.edu.bo");

        verify(correoService, never()).enviarEnlace(any(), any(), any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void solicitar_conUnaCuentaDesactivada_noGuardaNiEnviaNada() {
        ana.setActivo(false);

        service.solicitar(CORREO);

        verify(correoService, never()).enviarEnlace(any(), any(), any());
        assertThat(ana.getRestablecimientoTokenHash()).isNull();
    }

    @Test
    void solicitar_dosVeces_dejaSoloElHashDelUltimoToken() {
        service.solicitar(CORREO);
        service.solicitar(CORREO);

        ArgumentCaptor<String> tokens = ArgumentCaptor.forClass(String.class);
        verify(correoService, org.mockito.Mockito.times(2)).enviarEnlace(eq(CORREO), any(), tokens.capture());
        assertThat(tokens.getAllValues().get(0)).isNotEqualTo(tokens.getAllValues().get(1));
        assertThat(ana.getRestablecimientoTokenHash()).isEqualTo(tokenEnlace.hash(tokens.getAllValues().get(1)));
    }

    // ---- restablecer ----

    @Test
    void restablecer_conUnEnlaceVigente_cambiaLaContrasenaDesbloqueaLimpiaElEnlaceYCierraLasSesiones() {
        String token = tokenEnlace.generar();
        ana.iniciarRestablecimiento(tokenEnlace.hash(token), Instant.now().plus(Duration.ofMinutes(30)));
        ana.setIntentosFallidos(3);
        ana.setBloqueadoHasta(Instant.now().plus(Duration.ofHours(1)));
        when(usuarioRepository.findByRestablecimientoTokenHash(tokenEnlace.hash(token))).thenReturn(Optional.of(ana));

        ResultadoAuditado<Void> resultado = service.restablecer(token, "clave-nueva");

        assertThat(passwordEncoder.matches("clave-nueva", ana.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("clave-vieja", ana.getPasswordHash())).isFalse();
        assertThat(ana.getBloqueadoHasta()).isNull();
        assertThat(ana.getIntentosFallidos()).isZero();
        assertThat(ana.getRestablecimientoTokenHash()).isNull();
        assertThat(ana.getRestablecimientoExpiraEn()).isNull();
        verify(sesionService).revocarTodas(ana);
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.detalle().toString()).doesNotContain(token).doesNotContain("clave-nueva");
    }

    @Test
    void restablecer_conUnEnlaceDesconocido_lanzaErrAuth08SinCambiarNada() {
        when(usuarioRepository.findByRestablecimientoTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restablecer("token-inventado", "clave-nueva"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCodes.ERR_AUTH_08));

        verify(sesionService, never()).revocarTodas(any());
    }

    @Test
    void restablecer_conUnEnlaceVencido_lanzaErrAuth08SinCambiarLaContrasena() {
        String token = tokenEnlace.generar();
        ana.iniciarRestablecimiento(tokenEnlace.hash(token), Instant.now().minusSeconds(1));
        String hashAntes = ana.getPasswordHash();
        when(usuarioRepository.findByRestablecimientoTokenHash(tokenEnlace.hash(token))).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> service.restablecer(token, "clave-nueva"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCodes.ERR_AUTH_08));

        assertThat(ana.getPasswordHash()).isEqualTo(hashAntes);
        verify(sesionService, never()).revocarTodas(any());
    }

    @Test
    void restablecer_conUnaCuentaDesactivada_lanzaElMismoErrAuth08() {
        String token = tokenEnlace.generar();
        ana.iniciarRestablecimiento(tokenEnlace.hash(token), Instant.now().plus(Duration.ofMinutes(30)));
        ana.setActivo(false);
        when(usuarioRepository.findByRestablecimientoTokenHash(tokenEnlace.hash(token))).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> service.restablecer(token, "clave-nueva"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCodes.ERR_AUTH_08));
    }

    @Test
    void restablecer_unEnlaceUsadoDosVeces_elSegundoUsoEsRechazado() {
        String token = tokenEnlace.generar();
        ana.iniciarRestablecimiento(tokenEnlace.hash(token), Instant.now().plus(Duration.ofMinutes(30)));
        // El primer uso lo borra; el segundo ya no lo encuentra (la base ya no tiene ese hash).
        when(usuarioRepository.findByRestablecimientoTokenHash(tokenEnlace.hash(token)))
                .thenReturn(Optional.of(ana)).thenReturn(Optional.empty());

        service.restablecer(token, "clave-nueva");

        assertThatThrownBy(() -> service.restablecer(token, "otra-clave"))
                .isInstanceOf(ValidationException.class);
        assertThat(passwordEncoder.matches("clave-nueva", ana.getPasswordHash())).isTrue();
    }

    @Test
    void restablecer_siUnEnlaceAunNoVencidoSeUsa_elCodigoDelUsuarioSaleEnElResultado() {
        UUID codigo = UUID.randomUUID();
        Usuario conCodigo = mock(Usuario.class);
        String token = tokenEnlace.generar();
        when(conCodigo.tieneRestablecimientoVigente(any())).thenReturn(true);
        when(conCodigo.isActivo()).thenReturn(true);
        when(conCodigo.getCodigo()).thenReturn(codigo);
        when(conCodigo.getId()).thenReturn(7L);
        when(usuarioRepository.findByRestablecimientoTokenHash(tokenEnlace.hash(token)))
                .thenReturn(Optional.of(conCodigo));

        ResultadoAuditado<Void> resultado = service.restablecer(token, "clave-nueva");

        assertThat(resultado.actorCodigo()).isEqualTo(codigo);
        assertThat(resultado.entidadId()).isEqualTo(7L);
    }
}
