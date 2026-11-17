package com.comedoruagrm.backend.features.postulaciones.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.BorradorGuardadoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.BorradorResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.GuardarBorradorRequest;
import com.comedoruagrm.backend.features.postulaciones.exception.EnvioPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.service.BorradorPostulacionService;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP del borrador de la postulacion (CU17) con el service simulado: la forma del sobre, el codigo HTTP y
 * que el postulante salga del token. El `403` real por rol lo da `@PreAuthorize` en el service y se prueba en el
 * test unitario del service y con el stack levantado.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BorradorPostulacionControllerTest {

    private static final Instant GUARDADO = Instant.parse("2026-07-12T18:32:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorradorPostulacionService service;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID postulante = UUID.randomUUID();
    private final UUID postulacion = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(postulante))
                .thenReturn(Optional.of(new DatosUsuario(postulante, "POSTULANTE", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(postulante.toString()));
    }

    @Test
    void guardar_da200ConLaFechaYLoPendiente_yPasaElFormularioAlService() throws Exception {
        when(service.guardar(eq(postulante), eq(postulacion), any(GuardarBorradorRequest.class)))
                .thenReturn(new BorradorGuardadoResponse(postulacion, GUARDADO, 5));

        mockMvc.perform(put("/api/v1/postulaciones/{codigo}/borrador", postulacion).with(sesion())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dependientes\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.postulacion").value(postulacion.toString()))
                .andExpect(jsonPath("$.data.guardadoEn").exists())
                .andExpect(jsonPath("$.data.camposPendientes").value(5))
                .andExpect(jsonPath("$.error").doesNotExist());

        ArgumentCaptor<GuardarBorradorRequest> cuerpo = ArgumentCaptor.forClass(GuardarBorradorRequest.class);
        verify(service).guardar(eq(postulante), eq(postulacion), cuerpo.capture());
        org.assertj.core.api.Assertions.assertThat(cuerpo.getValue().dependientes()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(cuerpo.getValue().ingresosFamiliares()).isNull();
    }

    @Test
    void guardar_conUnDatoInvalido_da400ConElCodigoCatalogado() throws Exception {
        when(service.guardar(eq(postulante), eq(postulacion), any(GuardarBorradorRequest.class)))
                .thenThrow(new ValidationException(EnvioPostulacionErrorCodes.ERR_ENV_05, "Revisa el formulario"));

        mockMvc.perform(put("/api/v1/postulaciones/{codigo}/borrador", postulacion).with(sesion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"promedioAcademico\":" + new BigDecimal("150") + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_ENV_05"));
    }

    @Test
    void guardar_sinSesion_da401() throws Exception {
        mockMvc.perform(put("/api/v1/postulaciones/{codigo}/borrador", postulacion)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void miBorrador_conBorrador_da200ConElResumen() throws Exception {
        when(service.obtener(postulante)).thenReturn(Optional.of(new BorradorResumenResponse(postulacion,
                "Convocatoria 2026-II", "2026-II", GUARDADO.plusSeconds(86400), GUARDADO, 3, 1)));

        mockMvc.perform(get("/api/v1/postulaciones/mi-borrador").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.convocatoria").value("Convocatoria 2026-II"))
                .andExpect(jsonPath("$.data.camposPendientes").value(3))
                .andExpect(jsonPath("$.data.documentosPendientes").value(1));
    }

    @Test
    void miBorrador_sinBorrador_da200YElSobreOmiteData() throws Exception {
        when(service.obtener(postulante)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/postulaciones/mi-borrador").with(sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                // ApiResponse no serializa los nulos: sin borrador, `data` no viene (los clientes lo toman como null).
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
