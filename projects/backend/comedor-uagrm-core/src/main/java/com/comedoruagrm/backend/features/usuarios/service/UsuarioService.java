package com.comedoruagrm.backend.features.usuarios.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.common.auditoria.Auditable;
import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.features.auth.dto.RegistroRequest;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
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

    /** Intentos fallidos consecutivos que bloquean la cuenta (CU01, regla de bloqueo). */
    static final int MAX_INTENTOS_FALLIDOS = 5;

    // ? La escala exacta esta por definir con la DUBSS (CU01): 5 minutos la
    // ? primera vez y mas tiempo si se sigue fallando; el ultimo valor se repite.
    static final List<Duration> DURACIONES_BLOQUEO = List.of(
            Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(24));

    private final UsuarioRepository usuarioRepository;
    private final PostulanteRepository postulanteRepository;
    private final RolRepository rolRepository;
    private final CarreraRepository carreraRepository;
    private final PasswordEncoder passwordEncoder;

    // Hash de relleno: cuando el correo no existe se compara contra este para que la respuesta
    // tarde lo mismo que con un correo real (sin delatar cual de los dos datos fallo).
    private volatile String hashSenuelo;

    /**
     * `creado` es false cuando la cuenta ya existia (registro repetido): en ese
     * caso no se anota el alta en la bitacora.
     */
    public record ResultadoRegistro(Usuario usuario, boolean creado) implements Auditable {

        @Override
        public Long actorId() {
            return usuario.getId();
        }

        @Override
        public UUID actorCodigo() {
            return usuario.getCodigo();
        }

        @Override
        public Map<String, Object> detalle() {
            return Map.of("rol", usuario.getRol().getNombre());
        }

        @Override
        public boolean auditar() {
            return creado;
        }
    }

    /**
     * Crea el `usuario` y su `postulante` con rol POSTULANTE. El rol no es
     * elegible por el cliente y la contraseña se guarda solo como hash.
     *
     * * Idempotente (RNF-03): si llega de nuevo la misma peticion (mismo
     * * correo, CI y contraseña) devuelve la cuenta existente sin crear nada,
     * * asi el doble envio o el reintento no dan un 409.
     */
    @Transactional
    @Auditar(accion = BitacoraAcciones.REGISTRO_USUARIO, entidad = "usuario")
    public ResultadoRegistro registrarPostulante(RegistroRequest request) {
        String correo = normalizarCorreo(request.correo());

        var existente = usuarioRepository.findByCorreo(correo);
        if (existente.isPresent() && existente.get().getCi().equals(request.ci())
                && passwordEncoder.matches(request.contrasena(), existente.get().getPasswordHash())) {
            return new ResultadoRegistro(existente.get(), false);
        }

        Carrera carrera = carreraRepository.findByCodigo(request.carreraCodigo())
                .orElseThrow(() -> rechazar(new NotFoundException(UsuarioErrorCodes.ERR_US_04,
                        "La carrera indicada no existe")));
        if (usuarioRepository.existsByCi(request.ci())) {
            throw rechazar(new ConflictException(UsuarioErrorCodes.ERR_US_01, "Ya existe una cuenta con ese CI"));
        }
        if (existente.isPresent()) {
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
                    rol, request.ci(), correo, passwordEncoder.encode(request.contrasena()),
                    request.nombres(), request.apellidos()));
            postulanteRepository.saveAndFlush(new Postulante(
                    usuario, carrera, request.registroUniversitario(), request.telefono()));
            LOG.info("Postulante registrado", kv("module", "usuarios"), kv("userId", usuario.getCodigo()));
            return new ResultadoRegistro(usuario, true);
        } catch (DataIntegrityViolationException e) {
            throw traducirConflicto(e);
        }
    }

    /**
     * Verifica correo y contraseña y devuelve el usuario si puede iniciar sesion.
     *
     * * Orden: cuenta bloqueada (403, aunque la contraseña sea correcta) ->
     * * contraseña (401, el mismo mensaje exista o no el correo) -> cuenta
     * * desactivada (403). Un acierto reinicia el contador de intentos.
     * * `noRollbackFor`: el conteo de intentos fallidos tiene que guardarse
     * * aunque la operacion termine lanzando el error al cliente.
     * ! El mensaje de error nunca dice si fallo el correo o la contraseña.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public Usuario validarCredenciales(String correo, String contrasena) {
        var encontrado = usuarioRepository.findByCorreo(normalizarCorreo(correo));
        if (encontrado.isEmpty()) {
            passwordEncoder.matches(contrasena, hashSenuelo());
            throw credencialesIncorrectas();
        }
        Usuario usuario = encontrado.get();
        Instant ahora = Instant.now();

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
            throw cuentaBloqueada(usuario.getBloqueadoHasta(), ahora);
        }
        if (!passwordEncoder.matches(contrasena, usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario, ahora);
            throw credencialesIncorrectas();
        }
        if (!usuario.isActivo()) {
            LOG.warn("Intento de inicio de sesion con una cuenta desactivada",
                    kv("module", "usuarios"), kv("errorCode", AuthErrorCodes.ERR_AUTH_03),
                    kv("userId", usuario.getCodigo()));
            throw new ForbiddenException(AuthErrorCodes.ERR_AUTH_03, "Tu cuenta está desactivada. Contacta a la DUBSS.");
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueosConsecutivos(0);
        usuario.setBloqueadoHasta(null);
        return usuario;
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

    private String hashSenuelo() {
        if (hashSenuelo == null) {
            hashSenuelo = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return hashSenuelo;
    }

    // * El correo se compara en minuscula y sin espacios: el indice unico de la
    // * base distingue mayusculas, asi que sin normalizar "Ana@x.com" y
    // * "ana@x.com" serian dos cuentas.
    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    // Al llegar al maximo la cuenta se bloquea (la duracion crece con cada bloqueo seguido).
    private void registrarIntentoFallido(Usuario usuario, Instant ahora) {
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos < MAX_INTENTOS_FALLIDOS) {
            usuario.setIntentosFallidos(intentos);
            return;
        }
        int nivel = Math.min(usuario.getBloqueosConsecutivos(), DURACIONES_BLOQUEO.size() - 1);
        usuario.setBloqueadoHasta(ahora.plus(DURACIONES_BLOQUEO.get(nivel)));
        usuario.setBloqueosConsecutivos(usuario.getBloqueosConsecutivos() + 1);
        usuario.setIntentosFallidos(0);
        LOG.warn("Cuenta bloqueada temporalmente por intentos fallidos",
                kv("module", "usuarios"), kv("errorCode", AuthErrorCodes.ERR_AUTH_05),
                kv("userId", usuario.getCodigo()));
    }

    private UnauthorizedException credencialesIncorrectas() {
        LOG.warn("Inicio de sesion rechazado por credenciales incorrectas",
                kv("module", "usuarios"), kv("errorCode", AuthErrorCodes.ERR_AUTH_04));
        return new UnauthorizedException(AuthErrorCodes.ERR_AUTH_04, "Correo o contraseña incorrectos.");
    }

    private ForbiddenException cuentaBloqueada(Instant hasta, Instant ahora) {
        long minutos = Math.max(1, Duration.between(ahora, hasta).plusSeconds(59).toMinutes());
        LOG.warn("Inicio de sesion rechazado: cuenta bloqueada temporalmente",
                kv("module", "usuarios"), kv("errorCode", AuthErrorCodes.ERR_AUTH_05));
        return new ForbiddenException(AuthErrorCodes.ERR_AUTH_05,
                "Tu cuenta está bloqueada temporalmente por demasiados intentos fallidos. Vuelve a intentarlo en "
                        + minutos + (minutos == 1 ? " minuto." : " minutos."));
    }

    // * Dos registros simultaneos pueden pasar las verificaciones de arriba y
    // * chocar recien contra los indices unicos de la base: se traduce al mismo
    // * error de negocio que habria dado la verificacion. Una violacion de otro
    // * indice se deja subir tal cual (termina en el 500 generico).
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
        return e;
    }

    // WARN sin datos personales (ni CI, ni correo): solo el codigo de error.
    private <T extends BusinessException> T rechazar(T excepcion) {
        LOG.warn("Registro de postulante rechazado", kv("module", "usuarios"),
                kv("errorCode", excepcion.getErrorCode()));
        return excepcion;
    }
}
