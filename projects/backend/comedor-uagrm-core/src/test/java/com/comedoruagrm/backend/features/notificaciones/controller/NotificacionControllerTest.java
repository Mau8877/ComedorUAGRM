package com.comedoruagrm.backend.features.notificaciones.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP del aviso de prueba (CU-EXTRA-01) con el servicio simulado. La restriccion por rol
 * (`@PreAuthorize`) vive en el servicio real: se prueba en la suite de integracion.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificacionControllerTest {

    private static final String RUTA = "/api/v1/notificaciones/prueba";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacionService notificacionService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID codigo = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(codigo))
                .thenReturn(Optional.of(new DatosUsuario(codigo, "ADMINISTRADOR_DUBSS", true)));
    }

    @Test
    void enviarPrueba_sinSesion_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(post(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void enviarPrueba_conSesion_devuelve200SinDatosYPideLaPruebaDelUsuarioDelToken() throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("La notificación de prueba se está enviando"));

        verify(notificacionService).enviarPrueba(codigo);
    }
}
