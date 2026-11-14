package com.comedoruagrm.backend.features.comunicados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.comunicados.dto.ComunicadoResponse;
import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;
import com.comedoruagrm.backend.features.comunicados.service.ComunicadoService;
import com.comedoruagrm.backend.features.comunicados.service.ComunicadoService.PaginaComunicados;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de /api/v1/comunicados (CU21) con el service simulado. */
@SpringBootTest
@AutoConfigureMockMvc
class ComunicadoControllerTest {

    private static final String RUTA = "/api/v1/comunicados";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComunicadoService comunicadoService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID postulante = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
        when(provider.buscarPorCodigo(postulante))
                .thenReturn(Optional.of(new DatosUsuario(postulante, "POSTULANTE", true)));
    }

    private RequestPostProcessor sesion(UUID usuario) {
        return jwt().jwt(j -> j.subject(usuario.toString()));
    }

    private ComunicadoResponse comunicado() {
        return new ComunicadoResponse(UUID.randomUUID(), null, "Nuevo horario", "Desde el lunes",
                DestinatariosComunicado.TODOS, Instant.parse("2026-11-06T10:00:00Z"), 120);
    }

    @Test
    void publicar_da201ConElComunicadoYSuTotal() throws Exception {
        when(comunicadoService.publicar(eq(admin), any()))
                .thenReturn(new ResultadoAuditado<>(comunicado(), 1L, admin, 9L, Map.of(), true));

        mockMvc.perform(post(RUTA).with(sesion(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Nuevo horario\",\"contenido\":\"Desde el lunes\",\"destinatarios\":\"TODOS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalDestinatarios").value(120))
                .andExpect(jsonPath("$.data.destinatarios").value("TODOS"));
    }

    @Test
    void publicar_conCamposVacios_da400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA).with(sesion(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\" \",\"contenido\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void publicar_conDestinatariosDesconocidos_da400() throws Exception {
        mockMvc.perform(post(RUTA).with(sesion(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"a\",\"contenido\":\"b\",\"destinatarios\":\"NADIE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicar_adjudicados_da400ErrCom01() throws Exception {
        when(comunicadoService.publicar(eq(admin), any()))
                .thenThrow(new ValidationException("ERR_COM_01", "No soportado"));

        mockMvc.perform(post(RUTA).with(sesion(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"a\",\"contenido\":\"b\",\"destinatarios\":\"ADJUDICADOS\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_COM_01"));
    }

    @Test
    void publicar_sinSesion_da401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listar_devuelveElSobreConMeta() throws Exception {
        when(comunicadoService.listar(1, 20))
                .thenReturn(new PaginaComunicados(List.of(comunicado()), new PageMeta(1, 20, 1, 1)));

        mockMvc.perform(get(RUTA).with(sesion(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].titulo").value("Nuevo horario"))
                .andExpect(jsonPath("$.meta.totalPages").value(1));
    }
}
