package com.comedoruagrm.backend.features.contacto.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.comedoruagrm.backend.features.contacto.service.ContactoService;

/** Contrato HTTP del formulario de contacto: público (sin sesión) y validado. */
@SpringBootTest
@AutoConfigureMockMvc
class ContactoControllerTest {

    private static final String RUTA = "/api/v1/contacto";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactoService contactoService;

    @Test
    void enviar_sinSesionYConDatosValidos_devuelve200() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana@mail.com\",\"mensaje\":\"Hola\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.error").doesNotExist());
        verify(contactoService).enviar(any());
    }

    @Test
    void enviar_conCorreoInvalido_devuelve400SinEnviar() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"no-es-correo\",\"mensaje\":\"Hola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"));
        verify(contactoService, never()).enviar(any());
    }
}
