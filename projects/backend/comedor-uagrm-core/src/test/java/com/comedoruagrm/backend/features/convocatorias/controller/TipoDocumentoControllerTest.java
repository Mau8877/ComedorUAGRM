package com.comedoruagrm.backend.features.convocatorias.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.convocatorias.dto.TipoDocumentoResponse;
import com.comedoruagrm.backend.features.convocatorias.service.TipoDocumentoService;
import com.comedoruagrm.backend.features.convocatorias.service.TipoDocumentoService.PaginaTiposDocumento;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de /api/v1/tipos-documento con el servicio simulado (ver ConvocatoriaControllerTest). */
@SpringBootTest
@AutoConfigureMockMvc
class TipoDocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TipoDocumentoService service;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID adminCodigo = UUID.randomUUID();

    @BeforeEach
    void sesionDelAdministrador() {
        when(provider.buscarPorCodigo(adminCodigo))
                .thenReturn(Optional.of(new DatosUsuario(adminCodigo, "ADMINISTRADOR_DUBSS", true)));
    }

    @Test
    void listar_devuelve200ConArrayYMeta() throws Exception {
        when(service.listar(1, 20, null, null)).thenReturn(new PaginaTiposDocumento(
                List.of(new TipoDocumentoResponse(UUID.randomUUID(), "Carnet", null)), new PageMeta(1, 20, 1, 1)));

        mockMvc.perform(get("/api/v1/tipos-documento").with(jwt().jwt(j -> j.subject(adminCodigo.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Carnet"))
                .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-documento"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }
}
