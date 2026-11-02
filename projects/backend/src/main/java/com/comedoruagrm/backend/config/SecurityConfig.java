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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.comedoruagrm.backend.security.ApiAuthenticationEntryPoint;
import com.comedoruagrm.backend.security.FirebaseUsuarioFilter;
import com.comedoruagrm.backend.security.RateLimitFilter;
import com.comedoruagrm.backend.security.SecurityErrorWriter;
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
            // stateless y el ID token de Firebase viaja en el header Authorization.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Rate limit por IP sobre /api/v1/** -- ver SEGURIDAD_AUTH_BACKEND.md
            // y RateLimitFilter. Antes del filtro de autenticación: un cliente sin
            // token igual cuenta contra su límite, no es gratis intentar de más.
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // Verificación del ID token de Firebase (ver FirebaseJwtConfig). Los
            // rechazos de token salen en el sobre estándar de la API (ERR_AUTH_01).
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(authenticationEntryPoint))
            .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
            // Token válido -> usuario activo del sistema con su rol (ERR_AUTH_02/03).
            // Se instancia acá y no es un bean: ver FirebaseUsuarioFilter.
            .addFilterAfter(new FirebaseUsuarioFilter(usuarioProvider, securityErrorWriter),
                    BearerTokenAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // 1. Permitimos el tráfico a los endpoints de Actuator (expuestos en la raíz, ver
                // management.endpoints.web.base-path en application.properties) para health checks
                .requestMatchers("/health", "/health/**", "/prometheus").permitAll()
                // Swagger / OpenAPI (documentación de la API)
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // El formulario de registro necesita las carreras antes de que exista una
                // cuenta (ni token): única excepción de negocio, solo lectura de un catálogo.
                .requestMatchers(HttpMethod.GET, "/api/v1/carreras").permitAll()
                // 2. Cualquier otra ruta exige un token válido de Firebase
                .anyRequest().authenticated()
            );

        return http.build();
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
