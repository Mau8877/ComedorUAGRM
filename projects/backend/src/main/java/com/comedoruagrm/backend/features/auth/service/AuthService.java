package com.comedoruagrm.backend.features.auth.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.features.auth.dto.LoginRequest;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.auth.dto.SesionResponse;
import com.comedoruagrm.backend.features.auth.service.SesionService.SesionAbierta;
import com.comedoruagrm.backend.features.bitacora.BitacoraAcciones;
import com.comedoruagrm.backend.features.bitacora.service.BitacoraService;
import com.comedoruagrm.backend.features.usuarios.RolNombres;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService.ResultadoRegistro;

import lombok.RequiredArgsConstructor;

/**
 * Registro, inicio y cierre de sesion, y renovacion del token de acceso.
 * Orquesta los services de `usuarios` (cuentas y credenciales), `auth`
 * (sesiones) y `bitacora`; nunca toca sus repositorios.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioService usuarioService;
    private final SesionService sesionService;
    private final BitacoraService bitacoraService;

    /** `creado` es false si la cuenta ya existia (registro repetido, responde 200 y no anota el registro). */
    public record RegistroResultado(SesionResponse sesion, boolean creado) {
    }

    /** Crea la cuenta del Postulante y deja su sesion abierta (CU01, flujo A). */
    @Transactional
    public RegistroResultado registrar(RegistroRequest request, String ip) {
        ResultadoRegistro resultado = usuarioService.registrarPostulante(request);
        Usuario usuario = resultado.usuario();

        if (resultado.creado()) {
            bitacoraService.registrar(usuario, BitacoraAcciones.REGISTRO_USUARIO, "usuario", usuario.getId(),
                    Map.of("rol", RolNombres.POSTULANTE), ip);
        }
        return new RegistroResultado(abrirSesion(usuario, ip), resultado.creado());
    }

    /**
     * Inicio de sesion (CU01, flujo B). `noRollbackFor`: si las credenciales
     * fallan, el conteo de intentos fallidos igual se guarda.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public SesionResponse iniciarSesion(LoginRequest request, String ip) {
        Usuario usuario = usuarioService.validarCredenciales(request.correo(), request.contrasena());
        return abrirSesion(usuario, ip);
    }

    /** Canjea el refresh token por un par nuevo. No anota un acceso nuevo en la bitacora. */
    @Transactional
    public SesionResponse renovar(String refreshToken) {
        return aRespuesta(sesionService.renovar(refreshToken));
    }

    /** Cierra la sesion de este dispositivo (CU01, flujo C). */
    @Transactional
    public void cerrarSesion(String refreshToken) {
        sesionService.cerrar(refreshToken);
    }

    /** Recupera al usuario (recarga de pagina, reapertura de la app) sin anotar un acceso nuevo. */
    @Transactional(readOnly = true)
    public UsuarioResponse usuarioActual(UUID codigoUsuario) {
        return usuarioService.obtenerResumen(codigoUsuario);
    }

    private SesionResponse abrirSesion(Usuario usuario, String ip) {
        SesionAbierta abierta = sesionService.abrir(usuario);
        bitacoraService.registrar(usuario, BitacoraAcciones.INICIO_SESION, "usuario", usuario.getId(),
                Map.of("rol", usuario.getRol().getNombre()), ip);
        return aRespuesta(abierta);
    }

    private SesionResponse aRespuesta(SesionAbierta abierta) {
        return new SesionResponse(abierta.acceso().token(), abierta.refreshToken(), SesionResponse.BEARER,
                abierta.acceso().expiraEnSegundos(), usuarioService.aRespuesta(abierta.usuario()));
    }
}
