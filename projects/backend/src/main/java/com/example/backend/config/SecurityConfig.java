package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitamos CSRF temporalmente si vas a usar JWT más adelante para tu API
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 1. Permitimos el tráfico a los endpoints de Actuator para que Prometheus pueda leer
                .requestMatchers("/actuator/**").permitAll()
                // 2. Por ahora, protegemos cualquier otra ruta
                .anyRequest().authenticated()
            );

        return http.build();
    }
}