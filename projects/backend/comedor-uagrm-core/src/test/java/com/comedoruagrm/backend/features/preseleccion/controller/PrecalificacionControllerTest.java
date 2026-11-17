package com.comedoruagrm.backend.features.preseleccion.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.preseleccion.dto.AporteVariableResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.PasoTrazaResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.PrecalificacionResponse;
import com.comedoruagrm.backend.features.preseleccion.service.PrecalificacionDetalleService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de GET /api/v1/postulaciones/{codigo}/precalificacion (CU03) con el service simulado. */
@SpringBootTest
@AutoConfigureMockMvc
class PrecalificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrecalificacionDetalleService service;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID postulacion = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    private String ruta() {
        return "/api/v1/postulaciones/" + postulacion + "/precalificacion";
    }

    @Test
    void sinSesion_da401() throws Exception {
        mockMvc.perform(get(ruta())).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void devuelveElSobreConElPuntajeElAporteDeCadaVariableYLaTraza() throws Exception {
        when(service.obtener(postulacion)).thenReturn(new PrecalificacionResponse(postulacion, "Ana", "Rojas",
                "9802041", "Convocatoria 2026-II", "calificada", new BigDecimal("23.80"), new BigDecimal("70"),
                "v1.0", Instant.parse("2026-11-01T10:00:00Z"), 5L,
                List.of(new AporteVariableResponse("ingresos_familiares", new BigDecimal("1.0"),
                        new BigDecimal("28.0"), new BigDecimal("19.6"), new BigDecimal("49.00"))),
                List.of(), false, null, 0,
                List.of(new PasoTrazaResponse("Precalificación automática", null,
                        Instant.parse("2026-11-01T10:00:00Z")))));

        mockMvc.perform(get(ruta()).with(sesion())).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.puntaje").value(23.80))
                .andExpect(jsonPath("$.data.puntajeMaximo").value(70))
                .andExpect(jsonPath("$.data.modelo").value("v1.0"))
                .andExpect(jsonPath("$.data.posicion").value(5))
                .andExpect(jsonPath("$.data.variables[0].clave").value("ingresos_familiares"))
                .andExpect(jsonPath("$.data.variables[0].maximo").value(49.00))
                .andExpect(jsonPath("$.data.marcadaParaRevision").value(false))
                .andExpect(jsonPath("$.data.traza[0].paso").value("Precalificación automática"));
    }

    @Test
    void postulacionInexistente_da404ConSuCodigo() throws Exception {
        when(service.obtener(postulacion))
                .thenThrow(new NotFoundException("ERR_MOD_08", "La postulación no existe"));

        mockMvc.perform(get(ruta()).with(sesion())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_MOD_08"));
    }

    @Test
    void sinElRolDeAdministrador_da403() throws Exception {
        when(service.obtener(postulacion)).thenThrow(new AccessDeniedException("sin rol"));

        mockMvc.perform(get(ruta()).with(sesion())).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_07"));
    }
}
