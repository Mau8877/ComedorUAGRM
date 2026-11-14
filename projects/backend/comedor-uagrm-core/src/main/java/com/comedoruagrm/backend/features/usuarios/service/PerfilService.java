package com.comedoruagrm.backend.features.usuarios.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.auth.dto.CambiarContrasenaRequest;
import com.comedoruagrm.backend.features.auth.service.SesionService;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.usuarios.dto.ActualizarPerfilRequest;
import com.comedoruagrm.backend.features.usuarios.dto.CarreraResponse;
import com.comedoruagrm.backend.features.usuarios.dto.PerfilPostulanteResponse;
import com.comedoruagrm.backend.features.usuarios.dto.PerfilResponse;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Postulante;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.PostulanteRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Perfil del usuario autenticado (CU15, flujos C, D y E): consulta, actualizacion de los datos
 * de contacto (correo y, solo el Postulante, telefono) y cambio de contraseña.
 *
 * * El usuario sale siempre del token (`usuarioCodigo`), nunca del cuerpo. El CI y los demas datos
 * * no se editan: el request ni los trae (RF-10).
 * * Cambiar el correo exige la contraseña actual (reautenticacion, A7).
 * * Cada operacion que escribe se anota en la bitacora con `@Auditar` y el dueño de la cuenta
 * * como actor; `ACTUALIZACION_PERFIL` guarda solo QUE campos cambiaron, no sus valores.
 * ! Ni las contraseñas ni los refresh tokens se loguean ni van a la bitacora.
 */
@Service
@RequiredArgsConstructor
public class PerfilService {

    private static final Logger LOG = LoggerFactory.getLogger(PerfilService.class);

    private final UsuarioRepository usuarioRepository;
    private final PostulanteRepository postulanteRepository;
    private final UsuarioService usuarioService;
    private final SesionService sesionService;

    @Transactional(readOnly = true)
    public PerfilResponse obtener(UUID usuarioCodigo) {
        Usuario usuario = usuarioService.obtenerPorCodigo(usuarioCodigo);
        return aPerfil(usuario, postulanteDe(usuario));
    }

    /**
     * Actualiza correo y/o telefono; solo cambia lo que llega y es distinto del valor actual. Sin
     * cambios reales responde igual con el perfil y no anota la bitacora.
     */
    @Transactional
    @Auditar(accion = BitacoraAcciones.ACTUALIZACION_PERFIL, entidad = "usuario")
    public ResultadoAuditado<PerfilResponse> actualizar(UUID usuarioCodigo, ActualizarPerfilRequest request) {
        Usuario usuario = usuarioService.obtenerPorCodigo(usuarioCodigo);
        Postulante postulante = postulanteDe(usuario);
        List<String> campos = new ArrayList<>();

        if (request.correo() != null) {
            String correo = normalizarCorreo(request.correo());
            if (!correo.equals(usuario.getCorreo())) {
                usuarioService.verificarContrasenaActual(usuario, request.contrasenaActual());
                if (usuarioRepository.existsByCorreo(correo)) {
                    throw rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_02,
                            "Ya existe una cuenta con ese correo electrónico"));
                }
                usuario.setCorreo(correo);
                campos.add("correo");
            }
        }
        if (request.telefono() != null) {
            if (postulante == null) {
                throw rechazar(new ValidationException(UsuarioErrorCodes.ERR_US_13,
                        "Solo los postulantes tienen un teléfono para actualizar."));
            }
            String telefono = request.telefono().trim();
            if (!telefono.equals(postulante.getTelefono())) {
                postulante.setTelefono(telefono);
                campos.add("telefono");
            }
        }

        if (!campos.isEmpty()) {
            try {
                usuarioRepository.saveAndFlush(usuario);
                if (postulante != null) {
                    postulanteRepository.saveAndFlush(postulante);
                }
            } catch (DataIntegrityViolationException e) {
                throw traducirConflicto(e);
            }
            LOG.info("Perfil actualizado", kv("module", "usuarios"), kv("userId", usuario.getCodigo()),
                    kv("campos", campos));
        }
        return new ResultadoAuditado<>(aPerfil(usuario, postulante), usuario.getId(), usuario.getCodigo(),
                usuario.getId(), Map.of("campos", campos), !campos.isEmpty());
    }

    /**
     * Cambia la contraseña desde el perfil: verifica la actual, guarda la nueva y cierra las
     * sesiones de los demas dispositivos conservando la del `refreshTokenActual`. Sin ese token no
     * se puede saber cual conservar y se cierran todas.
     */
    @Transactional
    @Auditar(accion = BitacoraAcciones.CAMBIO_CONTRASENA, entidad = "usuario")
    public ResultadoAuditado<Void> cambiarContrasena(UUID usuarioCodigo, CambiarContrasenaRequest request,
            String refreshTokenActual) {
        Usuario usuario = usuarioService.obtenerPorCodigo(usuarioCodigo);
        usuarioService.cambiarContrasena(usuario, request.contrasenaActual(), request.contrasenaNueva());
        sesionService.revocarOtras(usuario, refreshTokenActual);

        LOG.info("Contraseña cambiada desde el perfil", kv("module", "usuarios"), kv("userId", usuario.getCodigo()));
        return new ResultadoAuditado<>(null, usuario.getId(), usuario.getCodigo(), usuario.getId(), Map.of(), true);
    }

    private Postulante postulanteDe(Usuario usuario) {
        return postulanteRepository.findByUsuarioId(usuario.getId()).orElse(null);
    }

    /** Mapeo inline; el rol y la carrera se leen de forma perezosa, dentro de la transaccion. */
    private PerfilResponse aPerfil(Usuario usuario, Postulante postulante) {
        PerfilPostulanteResponse datosPostulante = postulante == null ? null : new PerfilPostulanteResponse(
                postulante.getRegistroUniversitario(),
                new CarreraResponse(postulante.getCarrera().getCodigo(), postulante.getCarrera().getNombre(),
                        postulante.getCarrera().getFacultad()),
                postulante.getTelefono());
        return new PerfilResponse(usuario.getCodigo(), usuario.getNombres(), usuario.getApellidos(), usuario.getCi(),
                usuario.getCorreo(), usuario.getRol().getNombre(), usuario.isActivo(), datosPostulante);
    }

    // * El correo se guarda en minuscula y sin espacios: el indice unico distingue mayusculas.
    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    // * Dos cambios simultaneos al mismo correo pasan la verificacion y chocan en el indice unico.
    private RuntimeException traducirConflicto(DataIntegrityViolationException e) {
        if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_usuario_correo")) {
            return rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_02,
                    "Ya existe una cuenta con ese correo electrónico"));
        }
        return e;
    }

    // WARN sin datos personales (ni correo ni telefono): solo el codigo de error.
    private <T extends BusinessException> T rechazar(T excepcion) {
        LOG.warn("Actualizacion de perfil rechazada", kv("module", "usuarios"),
                kv("errorCode", excepcion.getErrorCode()));
        return excepcion;
    }
}
