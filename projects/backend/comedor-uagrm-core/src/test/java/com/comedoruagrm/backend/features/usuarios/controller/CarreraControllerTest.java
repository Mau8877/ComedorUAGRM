package com.comedoruagrm.backend.features.usuarios.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Usa las carreras que carga la migracion V20261102100100. */
@SpringBootTest
@AutoConfigureMockMvc
class CarreraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listar_sinToken_devuelve200ConArrayYMeta() throws Exception {
        mockMvc.perform(get("/api/v1/carreras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(20))
                .andExpect(jsonPath("$.data[0].codigo").exists())
                .andExpect(jsonPath("$.data[0].nombre").exists())
                .andExpect(jsonPath("$.data[0].facultad").exists())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20))
                .andExpect(jsonPath("$.meta.totalItems").value(71))
                .andExpect(jsonPath("$.meta.totalPages").value(4));
    }

    @Test
    void listar_conPageSizeGrande_devuelveTodasLasCarrerasEnUnaSolaPagina() throws Exception {
        mockMvc.perform(get("/api/v1/carreras").param("pageSize", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(71))
                .andExpect(jsonPath("$.meta.pageSize").value(200))
                .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    void listar_conPaginaInvalida_devuelve400ErrSys01() throws Exception {
        mockMvc.perform(get("/api/v1/carreras").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void otrasRutasDeLaApi_sinToken_siguenDando401() throws Exception {
        mockMvc.perform(get("/api/v1/otra-cosa"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }
}
