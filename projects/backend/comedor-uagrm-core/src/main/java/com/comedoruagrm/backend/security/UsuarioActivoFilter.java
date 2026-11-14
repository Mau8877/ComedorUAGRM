package com.comedoruagrm.backend.security;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Asocia cada peticion con token valido a un usuario activo del sistema y su
 * rol (las authorities son `ROLE_{rol}`).
 *
 * * Un token valido no alcanza: si el usuario ya no existe -> 401
 * * ERR_AUTH_02; si esta desactivado -> 403 ERR_AUTH_03. El estado `activo`
 * * se consulta en cada peticion, asi desactivar una cuenta surte efecto de
 * * inmediato aunque su token de acceso siga vigente.
 * * Se arma con `new` en SecurityConfig y NO es un bean: un Filter bean lo
 * * registra tambien Spring Boot en la cadena de servlets, donde correria
 * * antes de la autenticacion y marcaria la peticion como ya filtrada.
 * ! No loguea el token ni el correo.
 */
public class UsuarioActivoFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioActivoFilter.class);

    private static final String PREFIJO_API = "/api/v1/";

    private final UsuarioAutenticableProvider usuarios;
    private final SecurityErrorWriter errorWriter;

    public UsuarioActivoFilter(UsuarioAutenticableProvider usuarios, SecurityErrorWriter errorWriter) {
        this.usuarios = usuarios;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIJO_API);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        if (!(actual instanceof JwtAuthenticationToken jwtAuth)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID codigo = codigoDelToken(jwtAuth.getToken().getSubject());
        if (codigo == null) {
            errorWriter.escribir(response, HttpServletResponse.SC_UNAUTHORIZED, AuthErrorCodes.ERR_AUTH_01,
                    "Tu sesión no es válida. Inicia sesión de nuevo.");
            return;
        }

        Optional<DatosUsuario> datos = usuarios.buscarPorCodigo(codigo);
        if (datos.isEmpty()) {
            LOG.warn("Token valido de un usuario que ya no existe",
                    kv("module", "auth"), kv("errorCode", AuthErrorCodes.ERR_AUTH_02));
            errorWriter.escribir(response, HttpServletResponse.SC_UNAUTHORIZED, AuthErrorCodes.ERR_AUTH_02,
                    "Tu cuenta no está registrada en el sistema.");
            return;
        }
        if (!datos.get().activo()) {
            LOG.warn("Intento de acceso con una cuenta desactivada",
                    kv("module", "auth"), kv("errorCode", AuthErrorCodes.ERR_AUTH_03),
                    kv("userId", codigo));
            errorWriter.escribir(response, HttpServletResponse.SC_FORBIDDEN, AuthErrorCodes.ERR_AUTH_03,
                    "Tu cuenta está desactivada. Contacta a la DUBSS.");
            return;
        }

        DatosUsuario usuario = datos.get();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new UsuarioAutenticado(usuario.codigo(), usuario.rol()), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol()))));

        filterChain.doFilter(request, response);
    }

    private UUID codigoDelToken(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
