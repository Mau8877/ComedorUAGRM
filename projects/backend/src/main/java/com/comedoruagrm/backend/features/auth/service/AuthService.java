package com.comedoruagrm.backend.features.auth.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.bitacora.BitacoraAcciones;
import com.comedoruagrm.backend.features.bitacora.service.BitacoraService;
import com.comedoruagrm.backend.features.usuarios.RolNombres;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService.ResultadoRegistro;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.FirebaseSinRegistro;

import lombok.RequiredArgsConstructor;

/**
 * Registro e inicio de sesion. Firebase hace el login; este service solo
 * crea la cuenta del dominio, devuelve el usuario con su rol y deja
 * constancia en la bitacora. Usa el service de `usuarios` y el de
 * `bitacora`, nunca sus repositorios.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioService usuarioService;
    private final BitacoraService bitacoraService;

    /** `creado` es false si la cuenta ya existia (registro repetido, responde 200 y no anota nada). */
    public record RegistroResultado(UsuarioResponse usuario, boolean creado) {
    }

    @Transactional
    public RegistroResultado registrar(FirebaseSinRegistro token, RegistroRequest request, String ip) {
        if (token.correo() == null || token.correo().isBlank()) {
            throw new UnauthorizedException(AuthErrorCodes.ERR_AUTH_01, "Tu sesión no es válida. Inicia sesión de nuevo.");
        }

        ResultadoRegistro resultado = usuarioService.registrarPostulante(
                token.firebaseUid(), token.correo(), request);
        Usuario usuario = resultado.usuario();

        if (resultado.creado()) {
            bitacoraService.registrar(usuario, BitacoraAcciones.REGISTRO_USUARIO, "usuario", usuario.getId(),
                    Map.of("rol", RolNombres.POSTULANTE), ip);
        }
        return new RegistroResultado(usuarioService.aRespuesta(usuario), resultado.creado());
    }

    /**
     * Registro repetido de una cuenta que ya existe y esta activa (el filtro la
     * resolvio como usuario autenticado): se devuelve tal cual, sin crear ni anotar.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse registroExistente(UUID codigoUsuario) {
        return usuarioService.obtenerResumen(codigoUsuario);
    }

    /** Se llama una sola vez justo despues del login en Firebase: anota el acceso. */
    @Transactional
    public UsuarioResponse iniciarSesion(UUID codigoUsuario, String ip) {
        Usuario usuario = usuarioService.obtenerPorCodigo(codigoUsuario);
        bitacoraService.registrar(usuario, BitacoraAcciones.INICIO_SESION, "usuario", usuario.getId(),
                Map.of("rol", usuario.getRol().getNombre()), ip);
        return usuarioService.aRespuesta(usuario);
    }

    /** Recupera la sesion (recarga de pagina, reapertura de la app) sin anotar un acceso nuevo. */
    @Transactional(readOnly = true)
    public UsuarioResponse usuarioActual(UUID codigoUsuario) {
        return usuarioService.obtenerResumen(codigoUsuario);
    }
}
