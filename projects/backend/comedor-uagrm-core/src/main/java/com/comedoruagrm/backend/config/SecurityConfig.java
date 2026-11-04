package com.comedoruagrm.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.util.WebUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.comedoruagrm.backend.security.ApiAuthenticationEntryPoint;
import com.comedoruagrm.backend.security.AuthCookies;
import com.comedoruagrm.backend.security.RateLimitFilter;
import com.comedoruagrm.backend.security.RutasPublicas;
import com.comedoruagrm.backend.security.SecurityErrorWriter;
import com.comedoruagrm.backend.security.UsuarioActivoFilter;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    private final RateLimitFilter rateLimitFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final UsuarioAutenticableProvider usuarioProvider;
    private final SecurityErrorWriter securityErrorWriter;

    public SecurityConfig(
            RateLimitFilter rateLimitFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            UsuarioAutenticableProvider usuarioProvider,
            SecurityErrorWriter securityErrorWriter) {
        this.rateLimitFilter = rateLimitFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.usuarioProvider = usuarioProvider;
        this.securityErrorWriter = securityErrorWriter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF deshabilitado a propósito: no hay cookies de sesión, la API es
            // stateless y el token de acceso viaja en el header Authorization.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Rate limit por IP sobre /api/v1/** -- ver SEGURIDAD_AUTH_BACKEND.md
            // y RateLimitFilter. Antes del filtro de autenticación: un cliente sin
            // token igual cuenta contra su límite, no es gratis intentar de más.
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // Verificación del JWT propio (ver AutenticacionConfig). Los rechazos de
            // token salen en el sobre estándar de la API (ERR_AUTH_01).
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(bearerTokenResolver())
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(authenticationEntryPoint))
            .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
            // Token válido -> usuario activo del sistema con su rol (ERR_AUTH_02/03).
            // Se instancia acá y no es un bean: ver UsuarioActivoFilter.
            .addFilterAfter(new UsuarioActivoFilter(usuarioProvider, securityErrorWriter),
                    BearerTokenAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // 1. Permitimos el tráfico a los endpoints de Actuator (expuestos en la raíz, ver
                // management.endpoints.web.base-path en application.properties) para health checks
                .requestMatchers("/health", "/health/**", "/prometheus").permitAll()
                // Swagger / OpenAPI (documentación de la API)
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // Endpoints de negocio sin sesión (ver RutasPublicas y SEGURIDAD_AUTH_BACKEND.md):
                // las carreras (solo lectura de un catálogo, el formulario de registro las
                // necesita antes de tener cuenta) y registro / login / refresh / logout.
                .requestMatchers(HttpMethod.GET, RutasPublicas.CARRERAS).permitAll()
                .requestMatchers(HttpMethod.POST, RutasPublicas.REGISTRO, RutasPublicas.LOGIN,
                        RutasPublicas.REFRESH, RutasPublicas.LOGOUT).permitAll()
                // 2. Cualquier otra ruta exige un token de acceso válido
                .anyRequest().authenticated()
            );

        return http.build();
    }

    // * Una ruta pública ignora el header Authorization: sin esto, un token
    // * vencido que el cliente sigue mandando haría fallar el login con 401.
    // * El token se toma del header Authorization (mobile) y, si no viene, de la
    // * cookie HttpOnly de acceso (web).
    private BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver porDefecto = new DefaultBearerTokenResolver();
        return request -> {
            if (RutasPublicas.esPublica(request)) {
                return null;
            }
            String delHeader = porDefecto.resolve(request);
            if (delHeader != null) {
                return delHeader;
            }
            return WebUtils.getCookie(request, AuthCookies.ACCESO) != null
                    ? WebUtils.getCookie(request, AuthCookies.ACCESO).getValue() : null;
        };
    }

    // Permite que el frontend (React/Vite, en otro origen/puerto) pueda llamar a la API.
    // Orígenes permitidos configurables por app.cors.allowed-origins (ver application.properties).
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
