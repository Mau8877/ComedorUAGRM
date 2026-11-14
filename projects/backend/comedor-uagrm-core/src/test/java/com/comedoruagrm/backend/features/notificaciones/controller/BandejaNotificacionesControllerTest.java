package com.comedoruagrm.backend.features.notificaciones.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.notificaciones.dto.NotificacionResponse;
import com.comedoruagrm.backend.features.notificaciones.dto.ResumenNotificacionesResponse;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.BandejaNotificacionesService;
import com.comedoruagrm.backend.features.notificaciones.service.BandejaNotificacionesService.PaginaNotificaciones;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de la bandeja /api/v1/notificaciones/me (CU21) con el service simulado. */
@SpringBootTest
@AutoConfigureMockMvc
class BandejaNotificacionesControllerTest {

    private static final String RUTA = "/api/v1/notificaciones";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BandejaNotificacionesService bandeja;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID usuario = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(usuario))
                .thenReturn(Optional.of(new DatosUsuario(usuario, "POSTULANTE", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(usuario.toString()));
    }

    @Test
    void listar_sinSesion_da401() throws Exception {
        mockMvc.perform(get(RUTA + "/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void listar_devuelveLosAvisosConMeta_aCualquierRolConSesion() throws Exception {
        NotificacionResponse aviso = new NotificacionResponse(UUID.randomUUID(), TipoNotificacion.INFO, "Titulo",
                "Detalle", Instant.parse("2026-11-06T10:00:00Z"), false, null);
        when(bandeja.listar(usuario, 1, 10, "-fecha"))
                .thenReturn(new PaginaNotificaciones(List.of(aviso), new PageMeta(1, 10, 1, 1)));

        mockMvc.perform(get(RUTA + "/me?page=1&pageSize=10&sort=-fecha").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tipo").value("INFO"))
                .andExpect(jsonPath("$.data[0].detalle").value("Detalle"))
                .andExpect(jsonPath("$.data[0].leida").value(false))
                .andExpect(jsonPath("$.meta.pageSize").value(10));
    }

    @Test
    void resumen_devuelveElConteoDeNoLeidas() throws Exception {
        when(bandeja.resumir(usuario)).thenReturn(new ResumenNotificacionesResponse(8));

        mockMvc.perform(get(RUTA + "/me/resumen").with(sesion()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.noLeidas").value(8));
    }

    @Test
    void leer_da200ConDataNull_yUnAvisoAjenoDa404() throws Exception {
        UUID propio = UUID.randomUUID();
        UUID ajeno = UUID.randomUUID();
        doThrow(new NotFoundException("ERR_NOT_01", "La notificación no existe")).when(bandeja)
                .marcarLeida(eq(usuario), eq(ajeno));

        mockMvc.perform(post(RUTA + "/" + propio + "/leer").with(sesion()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(post(RUTA + "/" + ajeno + "/leer").with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_NOT_01"));
    }

    @Test
    void leerTodoYEliminarTodas_operanSobreElUsuarioDelToken() throws Exception {
        mockMvc.perform(post(RUTA + "/leer-todo").with(sesion())).andExpect(status().isOk());
        mockMvc.perform(delete(RUTA).with(sesion())).andExpect(status().isOk());

        verify(bandeja).marcarTodasLeidas(usuario);
        verify(bandeja).eliminarTodas(usuario);
        verify(bandeja, org.mockito.Mockito.never()).listar(any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), any());
    }
}
