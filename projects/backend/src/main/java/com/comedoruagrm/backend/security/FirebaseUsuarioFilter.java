package com.comedoruagrm.backend.security;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Asocia cada peticion con token valido de Firebase a un usuario activo del
 * sistema y su rol (las authorities son `ROLE_{rol}`).
 *
 * * Un token valido no alcanza: sin usuario registrado -> 401 ERR_AUTH_02;
 * * con el usuario desactivado -> 403 ERR_AUTH_03. La unica excepcion es el
 * * registro (POST /api/v1/auth/registro), que acepta un token sin usuario
 * * porque su proposito es crearlo.
 * * El estado `activo` se consulta en cada peticion, asi desactivar una
 * * cuenta surte efecto de inmediato aunque su token siga vigente.
 * * Se arma con `new` en SecurityConfig y NO es un bean: un Filter bean lo
 * * registra tambien Spring Boot en la cadena de servlets, donde correria
 * * antes de la autenticacion y marcaria la peticion como ya filtrada.
 * ! No loguea el token ni el correo.
 */
public class FirebaseUsuarioFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseUsuarioFilter.class);

    private static final String PREFIJO_API = "/api/v1/";
    private static final String RUTA_REGISTRO = "/api/v1/auth/registro";
    private static final String RUTA_CARRERAS = "/api/v1/carreras";

    private final UsuarioAutenticableProvider usuarios;
    private final SecurityErrorWriter errorWriter;

    public FirebaseUsuarioFilter(UsuarioAutenticableProvider usuarios, SecurityErrorWriter errorWriter) {
        this.usuarios = usuarios;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        boolean carrerasPublico = HttpMethod.GET.matches(request.getMethod()) && ruta.equals(RUTA_CARRERAS);
        return !ruta.startsWith(PREFIJO_API) || carrerasPublico;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        if (!(actual instanceof JwtAuthenticationToken jwtAuth)) {
            filterChain.doFilter(request, response);
            return;
        }

        Jwt jwt = jwtAuth.getToken();
        String firebaseUid = jwt.getSubject();
        Optional<DatosUsuario> datos = usuarios.buscarPorFirebaseUid(firebaseUid);

        if (datos.isEmpty()) {
            boolean esRegistro = HttpMethod.POST.matches(request.getMethod())
                    && request.getRequestURI().equals(RUTA_REGISTRO);
            if (!esRegistro) {
                LOG.warn("Token valido sin usuario registrado en el sistema",
                        kv("module", "auth"), kv("errorCode", AuthErrorCodes.ERR_AUTH_02));
                errorWriter.escribir(response, HttpServletResponse.SC_UNAUTHORIZED, AuthErrorCodes.ERR_AUTH_02,
                        "Tu cuenta no está registrada en el sistema.");
                return;
            }
            autenticar(new FirebaseSinRegistro(firebaseUid, jwt.getClaimAsString("email")), List.of());
        } else if (!datos.get().activo()) {
            LOG.warn("Intento de acceso con una cuenta desactivada",
                    kv("module", "auth"), kv("errorCode", AuthErrorCodes.ERR_AUTH_03),
                    kv("userId", datos.get().codigo()));
            errorWriter.escribir(response, HttpServletResponse.SC_FORBIDDEN, AuthErrorCodes.ERR_AUTH_03,
                    "Tu cuenta está desactivada. Contacta a la DUBSS.");
            return;
        } else {
            DatosUsuario usuario = datos.get();
            autenticar(new UsuarioAutenticado(usuario.codigo(), usuario.rol(), firebaseUid),
                    List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol())));
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(Object principal, List<SimpleGrantedAuthority> authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
    }
}
