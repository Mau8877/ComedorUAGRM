package com.comedoruagrm.backend.features.usuarios.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.usuarios.RolNombres;
import com.comedoruagrm.backend.features.usuarios.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioResponse;
import com.comedoruagrm.backend.features.usuarios.model.Carrera;
import com.comedoruagrm.backend.features.usuarios.model.Postulante;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.CarreraRepository;
import com.comedoruagrm.backend.features.usuarios.repository.PostulanteRepository;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.security.AuthErrorCodes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PostulanteRepository postulanteRepository;
    private final RolRepository rolRepository;
    private final CarreraRepository carreraRepository;

    /** `creado` es false cuando la cuenta ya existia (registro repetido). */
    public record ResultadoRegistro(Usuario usuario, boolean creado) {
    }

    /**
     * Crea el `usuario` y su `postulante` con rol POSTULANTE, una sola vez por
     * `firebaseUid`: si ya existe una cuenta con ese uid la devuelve sin crear
     * nada, asi el cliente puede reintentar el registro con el mismo token.
     * El `firebaseUid` y el `correo` salen del token verificado, nunca del
     * request, y el rol no es elegible por el cliente.
     */
    @Transactional
    public ResultadoRegistro registrarPostulante(String firebaseUid, String correo, RegistroRequest request) {
        var existente = usuarioRepository.findByFirebaseUid(firebaseUid);
        if (existente.isPresent()) {
            return new ResultadoRegistro(existente.get(), false);
        }

        Carrera carrera = carreraRepository.findByCodigo(request.carreraCodigo())
                .orElseThrow(() -> rechazar(new NotFoundException(UsuarioErrorCodes.ERR_US_04,
                        "La carrera indicada no existe")));
        if (usuarioRepository.existsByCi(request.ci())) {
            throw rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_01, "Ya existe una cuenta con ese CI"));
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_02,
                    "Ya existe una cuenta con ese correo electrónico"));
        }
        if (postulanteRepository.existsByRegistroUniversitario(request.registroUniversitario())) {
            throw rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_03,
                    "Ya existe una cuenta con ese registro universitario"));
        }

        Rol rol = rolRepository.findByNombre(RolNombres.POSTULANTE)
                .orElseThrow(() -> new IllegalStateException("No existe el rol POSTULANTE en la base de datos"));

        try {
            Usuario usuario = usuarioRepository.saveAndFlush(new Usuario(
                    rol, request.ci(), correo, firebaseUid, request.nombres(), request.apellidos()));
            postulanteRepository.saveAndFlush(new Postulante(
                    usuario, carrera, request.registroUniversitario(), request.telefono()));
            LOG.info("Postulante registrado", kv("module", "usuarios"), kv("userId", usuario.getCodigo()));
            return new ResultadoRegistro(usuario, true);
        } catch (DataIntegrityViolationException e) {
            throw traducirConflicto(e);
        }
    }

    /** El usuario autenticado, para registrar su acceso. */
    @Transactional(readOnly = true)
    public Usuario obtenerPorCodigo(UUID codigo) {
        return usuarioRepository.findByCodigo(codigo)
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.ERR_AUTH_02,
                        "Tu cuenta no está registrada en el sistema."));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerResumen(UUID codigo) {
        return aRespuesta(obtenerPorCodigo(codigo));
    }

    /** Mapeo inline (1:1 de campos); el rol se lee de forma perezosa, dentro de una transaccion. */
    public UsuarioResponse aRespuesta(Usuario usuario) {
        return new UsuarioResponse(usuario.getCodigo(), usuario.getNombres(), usuario.getApellidos(),
                usuario.getCorreo(), usuario.getRol().getNombre());
    }

    // * Dos registros simultaneos pueden pasar las verificaciones de arriba y
    // * chocar recien contra los indices unicos de la base: se traduce al mismo
    // * error de negocio que habria dado la verificacion. Una violacion de otro
    // * indice se deja subir tal cual (termina en el 500 generico). El choque de
    // * firebase_uid es el mismo token registrandose dos veces a la vez: la
    // * transaccion ya no sirve para releer, asi que se pide reintentar y el
    // * reintento cae en la rama idempotente (200).
    private RuntimeException traducirConflicto(DataIntegrityViolationException e) {
        String causa = String.valueOf(e.getMostSpecificCause().getMessage());
        if (causa.contains("uq_usuario_ci")) {
            return rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_01, "Ya existe una cuenta con ese CI"));
        }
        if (causa.contains("uq_usuario_correo")) {
            return rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_02,
                    "Ya existe una cuenta con ese correo electrónico"));
        }
        if (causa.contains("uq_postulante_registro_universitario")) {
            return rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_03,
                    "Ya existe una cuenta con ese registro universitario"));
        }
        if (causa.contains("uq_usuario_firebase_uid")) {
            return rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_05,
                    "Tu registro ya se está procesando. Vuelve a intentarlo en unos segundos."));
        }
        return e;
    }

    // WARN sin datos personales (ni CI, ni correo): solo el codigo de error.
    private <T extends BusinessException> T rechazar(T excepcion) {
        LOG.warn("Registro de postulante rechazado", kv("module", "usuarios"),
                kv("errorCode", excepcion.getErrorCode()));
        return excepcion;
    }
}
