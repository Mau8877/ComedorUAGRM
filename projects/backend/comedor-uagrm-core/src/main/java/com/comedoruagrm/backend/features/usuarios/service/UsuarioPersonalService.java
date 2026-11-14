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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.UnauthorizedException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.auth.service.SesionService;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.dto.ActualizarUsuarioPersonalRequest;
import com.comedoruagrm.backend.features.usuarios.dto.CambiarRolRequest;
import com.comedoruagrm.backend.features.usuarios.dto.CrearUsuarioPersonalRequest;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioCreadoResponse;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioPersonalResponse;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.security.AuthErrorCodes;

import lombok.RequiredArgsConstructor;

/**
 * Gestion de las cuentas del personal (CU14): listar, crear, editar, cambiar el
 * rol, desactivar y reactivar. Solo el Administrador DUBSS.
 *
 * * "Personal" = cualquier rol distinto de POSTULANTE; los Postulantes se
 * * registran solos (CU01) y para este servicio no existen (ERR_US_06).
 * * Cada operacion que escribe se anota en la bitacora con `@Auditar` y el
 * * administrador como actor (ver AUDITORIA_BITACORA_BACKEND.md).
 * * Una cuenta nunca se elimina: se desactiva (`activo = false`).
 * ! Ni la contraseña temporal ni su hash se loguean ni van a la bitacora.
 */
@Service
@RequiredArgsConstructor
public class UsuarioPersonalService {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioPersonalService.class);

    /** Los tres roles que se pueden asignar (todos menos POSTULANTE). */
    static final List<String> ROLES_PERSONAL = List.of(
            RolNombres.ADMINISTRADOR_DUBSS, RolNombres.DIRECTIVO, RolNombres.PERSONAL_COMEDOR);

    // Campo del `sort` (camelCase del DTO) -> ruta de la propiedad en el Model.
    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "apellidos", "apellidos",
            "ci", "ci",
            "correo", "correo",
            "rol", "rol.nombre",
            "creadoEn", "creadoEn");

    private static final String ORDEN_POR_DEFECTO = "-creadoEn";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SesionService sesionService;
    private final PasswordEncoder passwordEncoder;
    private final GeneradorContrasenaTemporal generadorContrasena;

    /** Una pagina de cuentas junto con su bloque `meta`. */
    public record PaginaUsuariosPersonal(List<UsuarioPersonalResponse> datos, PageMeta meta) {
    }

    // ---- Listar (flujo A) ----

    /** `page` es 1-indexed. Las lecturas no se loguean. */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional(readOnly = true)
    public PaginaUsuariosPersonal listar(int page, int pageSize, String search, String rol, Boolean activo,
            String sort) {
        if (page < 1 || pageSize < 1) {
            throw new ValidationException("ERR_SYS_01", "La página y el tamaño de página deben ser mayores a cero");
        }
        if (rol != null) {
            rolDelPersonal(rol);
        }

        Page<Usuario> pagina = usuarioRepository.buscarPersonal(
                patronDeBusqueda(search), rol, activo, PageRequest.of(page - 1, pageSize, orden(sort)));

        List<UsuarioPersonalResponse> datos = pagina.getContent().stream().map(this::aRespuesta).toList();
        return new PaginaUsuariosPersonal(datos,
                new PageMeta(page, pageSize, pagina.getTotalElements(), pagina.getTotalPages()));
    }

    // ---- Crear (flujo B) ----

    /**
     * Crea la cuenta (activa, con el rol indicado y sin fila de `postulante`) y devuelve la
     * contraseña temporal **una sola vez**.
     *
     * * Orden del diagrama de secuencia: datos y rol validos -> CI y correo libres
     * * -> recien entonces se genera la contraseña temporal y se crea la cuenta.
     * * Repetir el mismo alta da 409 (el CI ya esta registrado).
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.ALTA_PERSONAL, entidad = "usuario")
    public ResultadoAuditado<UsuarioCreadoResponse> crear(UUID actorCodigo, CrearUsuarioPersonalRequest request) {
        Usuario actor = actor(actorCodigo);
        String rolNombre = rolDelPersonal(request.rol());
        String correo = normalizarCorreo(request.correo());
        String ci = request.ci().trim();

        if (usuarioRepository.existsByCi(ci)) {
            throw rechazar("Alta de cuenta rechazada",
                    new ConflictException(UsuarioErrorCodes.ERR_US_01, "Ya existe una cuenta con ese CI"));
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw rechazar("Alta de cuenta rechazada", new ConflictException(UsuarioErrorCodes.ERR_US_02,
                    "Ya existe una cuenta con ese correo electrónico"));
        }

        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNombre + " en la base de datos"));
        String contrasenaTemporal = generadorContrasena.generar();

        Usuario cuenta;
        try {
            cuenta = usuarioRepository.saveAndFlush(new Usuario(rol, ci, correo,
                    passwordEncoder.encode(contrasenaTemporal), request.nombres().trim(), request.apellidos().trim()));
        } catch (DataIntegrityViolationException e) {
            throw traducirConflicto(e);
        }

        LOG.info("Cuenta del personal creada", kv("module", "usuarios"), kv("userId", cuenta.getCodigo()),
                kv("rol", rolNombre));
        return new ResultadoAuditado<>(UsuarioCreadoResponse.de(aRespuesta(cuenta), contrasenaTemporal),
                actor.getId(), actor.getCodigo(), cuenta.getId(), Map.of("rol", rolNombre), true);
    }

    // ---- Editar (flujo C) ----

    /**
     * Edicion parcial: solo cambian los campos que llegan (y que de verdad cambian). El CI y el rol
     * no se editan por acá. Un administrador puede editar sus propios datos. Si no cambia nada no se
     * anota una edicion vacia.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.EDICION_USUARIO, entidad = "usuario")
    public ResultadoAuditado<UsuarioPersonalResponse> editar(UUID actorCodigo, UUID codigo,
            ActualizarUsuarioPersonalRequest request) {
        Usuario actor = actor(actorCodigo);
        Usuario cuenta = buscarPersonal(codigo);
        List<String> modificados = new ArrayList<>();

        if (request.nombres() != null && !request.nombres().trim().equals(cuenta.getNombres())) {
            cuenta.setNombres(request.nombres().trim());
            modificados.add("nombres");
        }
        if (request.apellidos() != null && !request.apellidos().trim().equals(cuenta.getApellidos())) {
            cuenta.setApellidos(request.apellidos().trim());
            modificados.add("apellidos");
        }
        if (request.correo() != null) {
            String correo = normalizarCorreo(request.correo());
            if (!correo.equals(cuenta.getCorreo())) {
                var otra = usuarioRepository.findByCorreo(correo);
                if (otra.isPresent() && !otra.get().getId().equals(cuenta.getId())) {
                    throw rechazar("Edición de cuenta rechazada", new ConflictException(UsuarioErrorCodes.ERR_US_02,
                            "Ya existe una cuenta con ese correo electrónico"));
                }
                cuenta.setCorreo(correo);
                modificados.add("correo");
            }
        }

        if (!modificados.isEmpty()) {
            try {
                usuarioRepository.saveAndFlush(cuenta);
            } catch (DataIntegrityViolationException e) {
                throw traducirConflicto(e);
            }
            LOG.info("Cuenta del personal editada", kv("module", "usuarios"), kv("userId", cuenta.getCodigo()),
                    kv("campos", modificados));
        }
        return new ResultadoAuditado<>(aRespuesta(cuenta), actor.getId(), actor.getCodigo(), cuenta.getId(),
                Map.of("camposModificados", modificados), !modificados.isEmpty());
    }

    // ---- Cambiar rol (flujo D) ----

    /**
     * Cambia el rol y cierra las sesiones de la cuenta (A3: entra de nuevo y aterriza en el modulo
     * de su rol). Validaciones, en este orden: cuenta del personal (404), rol del personal (400),
     * no es la propia cuenta (403), no tiene ya ese rol (409) y, si era un Administrador activo, que
     * no sea el ultimo (403).
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.CAMBIO_ROL, entidad = "usuario")
    public ResultadoAuditado<UsuarioPersonalResponse> cambiarRol(UUID actorCodigo, UUID codigo,
            CambiarRolRequest request) {
        Usuario actor = actor(actorCodigo);
        Usuario cuenta = buscarPersonal(codigo);
        String rolNuevo = rolDelPersonal(request.rol());

        if (actor.getId().equals(cuenta.getId())) {
            throw rechazar("Cambio de rol rechazado", new ForbiddenException(UsuarioErrorCodes.ERR_US_08,
                    "No puedes cambiar tu propio rol"));
        }
        String rolAnterior = cuenta.getRol().getNombre();
        if (rolAnterior.equals(rolNuevo)) {
            throw rechazar("Cambio de rol rechazado",
                    new ConflictException(UsuarioErrorCodes.ERR_US_12, "La cuenta ya tiene ese rol"));
        }
        if (cuenta.isActivo() && RolNombres.ADMINISTRADOR_DUBSS.equals(rolAnterior)) {
            validarNoUltimoAdministrador(cuenta);
        }

        cuenta.setRol(rolRepository.findByNombre(rolNuevo)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + rolNuevo + " en la base de datos")));
        usuarioRepository.saveAndFlush(cuenta);
        sesionService.revocarTodas(cuenta);

        LOG.info("Rol de la cuenta cambiado", kv("module", "usuarios"), kv("userId", cuenta.getCodigo()),
                kv("rolAnterior", rolAnterior), kv("rolNuevo", rolNuevo));
        return new ResultadoAuditado<>(aRespuesta(cuenta), actor.getId(), actor.getCodigo(), cuenta.getId(),
                Map.of("rolAnterior", rolAnterior, "rolNuevo", rolNuevo), true);
    }

    // ---- Desactivar y reactivar (flujos E y F) ----

    /**
     * Desactiva la cuenta: pierde el acceso y se cierran sus sesiones; el registro se conserva (no se
     * elimina). Validaciones, en este orden (diagrama de secuencia): cuenta del personal (404), no es la
     * propia (403), no esta ya desactivada (409) y, si es Administrador, que no sea el ultimo activo (403).
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.DESACTIVACION_USUARIO, entidad = "usuario")
    public ResultadoAuditado<UsuarioPersonalResponse> desactivar(UUID actorCodigo, UUID codigo) {
        Usuario actor = actor(actorCodigo);
        Usuario cuenta = buscarPersonal(codigo);

        if (actor.getId().equals(cuenta.getId())) {
            throw rechazar("Desactivación rechazada", new ForbiddenException(UsuarioErrorCodes.ERR_US_08,
                    "No puedes desactivar tu propia cuenta"));
        }
        if (!cuenta.isActivo()) {
            throw rechazar("Desactivación rechazada",
                    new ConflictException(UsuarioErrorCodes.ERR_US_07, "La cuenta ya está desactivada"));
        }
        if (RolNombres.ADMINISTRADOR_DUBSS.equals(cuenta.getRol().getNombre())) {
            validarNoUltimoAdministrador(cuenta);
        }

        cuenta.setActivo(false);
        usuarioRepository.saveAndFlush(cuenta);
        sesionService.revocarTodas(cuenta);

        LOG.info("Cuenta del personal desactivada", kv("module", "usuarios"), kv("userId", cuenta.getCodigo()));
        return new ResultadoAuditado<>(aRespuesta(cuenta), actor.getId(), actor.getCodigo(), cuenta.getId(),
                Map.of(), true);
    }

    /**
     * Reactiva la cuenta con el rol que tenia (A6). No restaura sesiones: la persona inicia sesion de
     * nuevo. Una cuenta ya activa da 409.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.REACTIVACION_USUARIO, entidad = "usuario")
    public ResultadoAuditado<UsuarioPersonalResponse> reactivar(UUID actorCodigo, UUID codigo) {
        Usuario actor = actor(actorCodigo);
        Usuario cuenta = buscarPersonal(codigo);

        if (cuenta.isActivo()) {
            throw rechazar("Reactivación rechazada",
                    new ConflictException(UsuarioErrorCodes.ERR_US_07, "La cuenta ya está activa"));
        }

        cuenta.setActivo(true);
        usuarioRepository.saveAndFlush(cuenta);

        LOG.info("Cuenta del personal reactivada", kv("module", "usuarios"), kv("userId", cuenta.getCodigo()));
        return new ResultadoAuditado<>(aRespuesta(cuenta), actor.getId(), actor.getCodigo(), cuenta.getId(),
                Map.of(), true);
    }

    // ---- Ayudas ----

    /** El administrador que actua, resuelto desde el `sub` del token (nunca desde el body). */
    private Usuario actor(UUID actorCodigo) {
        return usuarioRepository.findByCodigo(actorCodigo)
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCodes.ERR_AUTH_02,
                        "Tu cuenta no está registrada en el sistema."));
    }

    /** La cuenta del personal con ese codigo; una cuenta inexistente, eliminada o de un Postulante es 404. */
    Usuario buscarPersonal(UUID codigo) {
        return usuarioRepository.findPersonalByCodigo(codigo)
                .orElseThrow(() -> rechazar("Cuenta del personal no encontrada",
                        new NotFoundException(UsuarioErrorCodes.ERR_US_06, "La cuenta indicada no existe")));
    }

    /** Devuelve el nombre del rol si es uno de los tres del personal; si no, 400 ERR_US_10. */
    private String rolDelPersonal(String nombre) {
        if (nombre == null || !ROLES_PERSONAL.contains(nombre.trim())) {
            throw rechazar("Rol no válido para el personal", new ValidationException(UsuarioErrorCodes.ERR_US_10,
                    "El rol indicado no es válido para el personal"));
        }
        return nombre.trim();
    }

    /**
     * Que la cuenta no sea el unico Administrador activo (A5). Cuenta los administradores activos
     * bloqueando sus filas hasta el fin de la transaccion: dos operaciones simultaneas no pueden dejar
     * a cero.
     */
    void validarNoUltimoAdministrador(Usuario cuenta) {
        boolean quedaOtro = usuarioRepository.bloquearAdministradoresActivos().stream()
                .anyMatch(administrador -> !administrador.getId().equals(cuenta.getId()));
        if (!quedaOtro) {
            throw rechazar("Operación rechazada: era el último Administrador activo", new ForbiddenException(
                    UsuarioErrorCodes.ERR_US_09, "No se puede dejar al sistema sin un Administrador DUBSS activo"));
        }
    }

    private Sort orden(String sort) {
        String valor = (sort == null || sort.isBlank()) ? ORDEN_POR_DEFECTO : sort.trim();
        Sort.Direction direccion = valor.startsWith("-") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String campo = valor.startsWith("-") ? valor.substring(1) : valor;
        String ruta = CAMPOS_ORDENABLES.get(campo);
        if (ruta == null) {
            throw rechazar("Listado de cuentas rechazado", new ValidationException(UsuarioErrorCodes.ERR_US_11,
                    "No se puede ordenar por «" + campo + "». Campos válidos: "
                            + String.join(", ", CAMPOS_ORDENABLES.keySet().stream().sorted().toList())));
        }
        // El `id` desempata para que la paginacion sea estable.
        return Sort.by(direccion, ruta).and(Sort.by(direccion, Usuario::getId));
    }

    private String patronDeBusqueda(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }

    // El correo se compara en minuscula y sin espacios (mismo criterio que el registro).
    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private UsuarioPersonalResponse aRespuesta(Usuario cuenta) {
        return new UsuarioPersonalResponse(cuenta.getCodigo(), cuenta.getNombres(), cuenta.getApellidos(),
                cuenta.getCi(), cuenta.getCorreo(), cuenta.getRol().getNombre(), cuenta.isActivo());
    }

    // Dos altas o ediciones simultaneas pueden chocar recien contra los indices unicos: mismo 409 que
    // habria dado la verificacion. Una violacion de otro indice sube tal cual (termina en el 500 generico).
    private RuntimeException traducirConflicto(DataIntegrityViolationException e) {
        String causa = String.valueOf(e.getMostSpecificCause().getMessage());
        if (causa.contains("uq_usuario_ci")) {
            return rechazar("Alta de cuenta rechazada",
                    new ConflictException(UsuarioErrorCodes.ERR_US_01, "Ya existe una cuenta con ese CI"));
        }
        if (causa.contains("uq_usuario_correo")) {
            return rechazar("Operación de cuenta rechazada", new ConflictException(UsuarioErrorCodes.ERR_US_02,
                    "Ya existe una cuenta con ese correo electrónico"));
        }
        return e;
    }

    // WARN sin datos personales (ni CI, ni correo): solo el codigo de error.
    private <T extends BusinessException> T rechazar(String mensaje, T excepcion) {
        LOG.warn(mensaje, kv("module", "usuarios"), kv("errorCode", excepcion.getErrorCode()));
        return excepcion;
    }
}
