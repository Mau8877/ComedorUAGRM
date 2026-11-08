package com.comedoruagrm.backend.features.finalistas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.response.PageMeta;
import com.comedoruagrm.backend.features.finalistas.dto.FinalistaResponse;
import com.comedoruagrm.backend.features.finalistas.dto.ResultadoNotificacionResponse;
import com.comedoruagrm.backend.features.finalistas.service.FinalistaService;
import com.comedoruagrm.backend.features.finalistas.service.FinalistaService.PaginaFinalistas;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de /api/v1/finalistas (CU21) con el service simulado. */
@SpringBootTest
@AutoConfigureMockMvc
class FinalistaControllerTest {

    private static final String RUTA = "/api/v1/finalistas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinalistaService finalistaService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();
    private final UUID finalista = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    private FinalistaResponse fila() {
        return new FinalistaResponse(finalista, 1, "Ana", "Rojas", "9802041", new BigDecimal("95.0"), "SIN_ENVIAR",
                null, null, null, "SIN_AUDITAR", null, "calificada");
    }

    @Test
    void listar_sinSesion_da401() throws Exception {
        mockMvc.perform(get(RUTA + "?filter[convocatoriaId]=" + convocatoria)).andExpect(status().isUnauthorized());
    }

    @Test
    void listar_devuelveElSobreConLaListaYSuMeta_yUsaLosValoresPorDefecto() throws Exception {
        when(finalistaService.listar(eq(convocatoria), any(), eq(1), eq(20)))
                .thenReturn(new PaginaFinalistas(List.of(fila()), new PageMeta(1, 20, 1, 1)));

        mockMvc.perform(get(RUTA + "?filter[convocatoriaId]=" + convocatoria).with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].codigo").value(finalista.toString()))
                .andExpect(jsonPath("$.data[0].estadoNotificacion").value("SIN_ENVIAR"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void listar_sinLaConvocatoria_da400() throws Exception {
        mockMvc.perform(get(RUTA).with(sesion())).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"));
    }

    @Test
    void listar_conSortOFiltroNoSoportado_da400ErrFin05() throws Exception {
        mockMvc.perform(get(RUTA + "?filter[convocatoriaId]=" + convocatoria + "&sort=-puntaje").with(sesion()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_FIN_05"));
        mockMvc.perform(get(RUTA + "?filter[convocatoriaId]=" + convocatoria + "&filter[estado]=x").with(sesion()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_FIN_05"));
    }

    @Test
    void listar_conUnaConvocatoriaInexistente_da404() throws Exception {
        when(finalistaService.listar(eq(convocatoria), any(), eq(1), eq(20)))
                .thenThrow(new NotFoundException("ERR_FIN_02", "La convocatoria no existe"));

        mockMvc.perform(get(RUTA + "?filter[convocatoriaId]=" + convocatoria).with(sesion()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_FIN_02"));
    }

    @Test
    void notificar_conDatosCompletos_da200ConElResultado() throws Exception {
        when(finalistaService.notificar(eq(admin), any())).thenReturn(new ResultadoAuditado<>(
                new ResultadoNotificacionResponse(1, 1, 0), 1L, admin, null, java.util.Map.of(), true));
        String cuerpo = """
                {"finalistas":["%s"],"plazoEntrega":"%s","lugarHorario":"DUBSS oficina 3","documentos":"CI"}
                """.formatted(finalista, LocalDate.now().plusDays(7));

        mockMvc.perform(post(RUTA + "/notificaciones").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.solicitados").value(1))
                .andExpect(jsonPath("$.data.enviados").value(1))
                .andExpect(jsonPath("$.data.fallidos").value(0));
    }

    @Test
    void notificar_conCamposVacios_da400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA + "/notificaciones").with(sesion()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"finalistas\":[],\"lugarHorario\":\" \",\"documentos\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void registrarEntrega_da200ConElFinalistaActualizado() throws Exception {
        when(finalistaService.registrarEntrega(eq(admin), eq(finalista), any())).thenReturn(new ResultadoAuditado<>(
                fila(), 1L, admin, 10L, java.util.Map.of(), true));

        mockMvc.perform(patch(RUTA + "/" + finalista + "/entrega").with(sesion())
                .contentType(MediaType.APPLICATION_JSON).content("{\"observaciones\":\"Todo completo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigo").value(finalista.toString()));
        verify(finalistaService).registrarEntrega(eq(admin), eq(finalista), any());
    }
}
