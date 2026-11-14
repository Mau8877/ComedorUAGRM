package com.comedoruagrm.backend.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * La falta de rol sale como 403 ERR_AUTH_07 en el sobre de la API (no una
 * pagina vacia). Usa un endpoint de prueba protegido con `@PreAuthorize`,
 * igual que lo hara `UsuarioPersonalService`.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiAccessDeniedHandlerTest.ControllerDePrueba.class)
class ApiAccessDeniedHandlerTest {

    private final UUID codigo = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    @TestConfiguration
    static class ControllerDePrueba {

        @Bean
        PruebaRolesController pruebaRolesController() {
            return new PruebaRolesController();
        }
    }

    @RestController
    static class PruebaRolesController {

        @GetMapping("/api/v1/prueba-roles/solo-admin")
        @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
        public String soloAdmin() {
            return "ok";
        }
    }

    @Test
    void usuarioSinElRol_recibe403ErrAuth07EnElSobre() throws Exception {
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.of(new DatosUsuario(codigo, "DIRECTIVO", true)));

        mockMvc.perform(get("/api/v1/prueba-roles/solo-admin").with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void usuarioConElRol_pasa() throws Exception {
        when(provider.buscarPorCodigo(codigo))
                .thenReturn(Optional.of(new DatosUsuario(codigo, "ADMINISTRADOR_DUBSS", true)));

        mockMvc.perform(get("/api/v1/prueba-roles/solo-admin").with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void sinToken_sigueDando401ErrAuth01() throws Exception {
        mockMvc.perform(get("/api/v1/prueba-roles/solo-admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }
}
