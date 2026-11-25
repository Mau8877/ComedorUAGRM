package com.comedoruagrm.backend.features.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.features.auth.service.SesionService;
import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;

/**
 * Pruebas de caja blanca de CU-14 (capitulo 9, 9.3.2): un test por camino basico de
 * `UsuarioPersonalService.desactivar` (C1 a C4).
 */
class UsuarioPersonalServiceCajaBlancaTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final RolRepository rolRepository = mock(RolRepository.class);
    private final SesionService sesionService = mock(SesionService.class);
    private final GeneradorContrasenaTemporal generador = mock(GeneradorContrasenaTemporal.class);
    private final UsuarioPersonalService service = new UsuarioPersonalService(usuarioRepository, rolRepository,
            sesionService, new BCryptPasswordEncoder(4), generador);

    private final UUID adminCodigo = UUID.randomUUID();
    private final Usuario admin = cuenta(1L, RolNombres.ADMINISTRADOR_DUBSS, true, adminCodigo);

    private static Usuario cuenta(long id, String rol, boolean activo, UUID codigo) {
        Usuario cuenta = mock(Usuario.class);
        Rol rolCuenta = mock(Rol.class);
        when(rolCuenta.getNombre()).thenReturn(rol);
        when(cuenta.getId()).thenReturn(id);
        when(cuenta.getCodigo()).thenReturn(codigo);
        when(cuenta.getRol()).thenReturn(rolCuenta);
        when(cuenta.isActivo()).thenReturn(activo);
        return cuenta;
    }

    private Usuario cuentaDelPersonal(long id, String rol, boolean activo) {
        Usuario cuenta = cuenta(id, rol, activo, UUID.randomUUID());
        when(usuarioRepository.findPersonalByCodigo(cuenta.getCodigo())).thenReturn(Optional.of(cuenta));
        return cuenta;
    }

    private void preparar() {
        when(usuarioRepository.findByCodigo(adminCodigo)).thenReturn(Optional.of(admin));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void desactivar_C1_laPropiaCuenta_lanzaErrUs08() {
        preparar();
        when(usuarioRepository.findPersonalByCodigo(adminCodigo)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.desactivar(adminCodigo, adminCodigo))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_08));
        verify(admin, never()).setActivo(false);
        verifyNoInteractions(sesionService);
    }

    @Test
    void desactivar_C2_cuentaYaDesactivada_lanzaErrUs07() {
        preparar();
        Usuario cuenta = cuentaDelPersonal(5L, RolNombres.DIRECTIVO, false);

        assertThatThrownBy(() -> service.desactivar(adminCodigo, cuenta.getCodigo()))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_07));
        verifyNoInteractions(sesionService);
    }

    @Test
    void desactivar_C3_cuentaDeAdministrador_compruebaElUltimoAdministradorYDesactiva() {
        preparar();
        Usuario otroAdmin = cuentaDelPersonal(5L, RolNombres.ADMINISTRADOR_DUBSS, true);
        when(usuarioRepository.bloquearAdministradoresActivos()).thenReturn(List.of(otroAdmin, admin));

        service.desactivar(adminCodigo, otroAdmin.getCodigo());

        verify(usuarioRepository).bloquearAdministradoresActivos();
        verify(otroAdmin).setActivo(false);
        verify(sesionService).revocarTodas(otroAdmin);
    }

    @Test
    void desactivar_C4_cuentaDeOtroRol_desactivaYRevocaSusSesiones() {
        preparar();
        Usuario cuenta = cuentaDelPersonal(5L, RolNombres.DIRECTIVO, true);

        service.desactivar(adminCodigo, cuenta.getCodigo());

        verify(usuarioRepository, never()).bloquearAdministradoresActivos();
        verify(cuenta).setActivo(false);
        verify(sesionService).revocarTodas(cuenta);
    }
}
