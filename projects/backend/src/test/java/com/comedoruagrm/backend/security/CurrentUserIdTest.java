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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comedoruagrm.backend.common.response.ApiResponse;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Recorre el filtro de usuario + @CurrentUserId + seguridad por metodo con un
 * controller que existe solo en los tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(CurrentUserIdTest.ControllerDePrueba.class)
class CurrentUserIdTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    @RestController
    static class ControllerDePrueba {

        @GetMapping("/api/v1/test/current-user-id")
        ApiResponse<UUID> yo(@CurrentUserId UUID userId) {
            return ApiResponse.success(userId, "ok");
        }

        @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
        @GetMapping("/api/v1/test/solo-admin")
        ApiResponse<String> soloAdmin() {
            return ApiResponse.success("admin", "ok");
        }
    }

    @Test
    void currentUserId_conUsuarioActivo_recibeElCodigoDelUsuario() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", true)));

        mockMvc.perform(get("/api/v1/test/current-user-id").with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").value(codigo.toString()));
    }

    @Test
    void currentUserId_sinToken_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(get("/api/v1/test/current-user-id"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void currentUserId_conTokenSinCuenta_devuelve401ErrAuth02() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/test/current-user-id").with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_02"));
    }

    @Test
    void currentUserId_conCuentaDesactivada_devuelve403ErrAuth03() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(provider.buscarPorCodigo(codigo))
                .thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", false)));

        mockMvc.perform(get("/api/v1/test/current-user-id").with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_03"));
    }

    @Test
    void preAuthorize_conElRolCorrecto_dejaPasar() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(provider.buscarPorCodigo(codigo))
                .thenReturn(Optional.of(new DatosUsuario(codigo, "ADMINISTRADOR_DUBSS", true)));

        mockMvc.perform(get("/api/v1/test/solo-admin").with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("admin"));
    }
}
