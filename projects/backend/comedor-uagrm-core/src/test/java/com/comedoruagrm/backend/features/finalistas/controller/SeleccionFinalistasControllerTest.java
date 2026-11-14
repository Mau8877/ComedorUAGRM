package com.comedoruagrm.backend.features.finalistas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
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

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.finalistas.service.SeleccionFinalistasService;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionFinalistasResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionFinalistasResponse.FinalistaSeleccionado;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionarFinalistasRequest;
import com.comedoruagrm.backend.features.postulaciones.exception.AdjudicacionErrorCodes;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/**
 * Contrato HTTP de POST /api/v1/convocatorias/{codigo}/finalistas (CU05) con el service simulado. El `403` por
 * rol lo comprueba el service: se prueba en `AdjudicacionIntegrationTest`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeleccionFinalistasControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeleccionFinalistasService service;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID admin = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(admin))
                .thenReturn(Optional.of(new DatosUsuario(admin, "ADMINISTRADOR_DUBSS", true)));
    }

    private RequestPostProcessor sesion() {
        return jwt().jwt(j -> j.subject(admin.toString()));
    }

    private String ruta() {
        return "/api/v1/convocatorias/" + convocatoria + "/finalistas";
    }

    private ResultadoAuditado<SeleccionFinalistasResponse> resultado(int cantidad) {
        List<FinalistaSeleccionado> lista = java.util.stream.IntStream.rangeClosed(1, cantidad)
                .mapToObj(i -> new FinalistaSeleccionado(UUID.randomUUID(), UUID.randomUUID(), i)).toList();
        return new ResultadoAuditado<>(new SeleccionFinalistasResponse(convocatoria, cantidad, lista), 1L, admin,
                null, Map.of(), true);
    }

    @Test
    void sinSesion_da401() throws Exception {
        mockMvc.perform(post(ruta()).contentType(JSON).content("{\"cantidad\":3}"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).seleccionar(any(), any(), any());
    }

    @Test
    void conCantidad_da201ConElSobreYLosFinalistasEnOrden() throws Exception {
        when(service.seleccionar(eq(admin), eq(convocatoria), any())).thenReturn(resultado(3));

        mockMvc.perform(post(ruta()).with(sesion()).contentType(JSON).content("{\"cantidad\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Finalistas seleccionados correctamente"))
                .andExpect(jsonPath("$.data.convocatoria").value(convocatoria.toString()))
                .andExpect(jsonPath("$.data.seleccionados").value(3))
                .andExpect(jsonPath("$.data.finalistas.length()").value(3))
                .andExpect(jsonPath("$.data.finalistas[0].posicion").value(1))
                .andExpect(jsonPath("$.data.finalistas[2].posicion").value(3))
                .andExpect(jsonPath("$.data.finalistas[0].codigo").isNotEmpty())
                .andExpect(jsonPath("$.data.finalistas[0].id").doesNotExist());
        ArgumentCaptor<SeleccionarFinalistasRequest> pedido = ArgumentCaptor.forClass(SeleccionarFinalistasRequest.class);
        verify(service).seleccionar(eq(admin), eq(convocatoria), pedido.capture());
        org.assertj.core.api.Assertions.assertThat(pedido.getValue().cantidad()).isEqualTo(3);
    }

    @Test
    void sinCuerpoOConCuerpoVacio_da201YPasaCantidadNula() throws Exception {
        when(service.seleccionar(eq(admin), eq(convocatoria), any())).thenReturn(resultado(2));

        mockMvc.perform(post(ruta()).with(sesion())).andExpect(status().isCreated());
        mockMvc.perform(post(ruta()).with(sesion()).contentType(JSON).content("{}")).andExpect(status().isCreated());

        ArgumentCaptor<SeleccionarFinalistasRequest> pedido = ArgumentCaptor.forClass(SeleccionarFinalistasRequest.class);
        verify(service, org.mockito.Mockito.times(2)).seleccionar(eq(admin), eq(convocatoria), pedido.capture());
        org.assertj.core.api.Assertions.assertThat(pedido.getAllValues()).allSatisfy(p -> org.assertj.core.api.Assertions
                .assertThat(p.cantidad()).isNull());
    }

    @Test
    void conCantidadCeroONegativa_da400ErrSys01() throws Exception {
        for (String cuerpo : new String[] {"{\"cantidad\":0}", "{\"cantidad\":-4}"}) {
            mockMvc.perform(post(ruta()).with(sesion()).contentType(JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("failed"))
                    .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        }
        verify(service, never()).seleccionar(any(), any(), any());
    }

    // Un valor que no es un entero ni se llega a leer: lo responde el manejador global de requests mal formados.
    @Test
    void conCantidadNoNumerica_da400ErrSys05() throws Exception {
        for (String cuerpo : new String[] {"{\"cantidad\":\"muchos\"}", "{\"cantidad\":true}"}) {
            mockMvc.perform(post(ruta()).with(sesion()).contentType(JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("failed"))
                    .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
        }
        verify(service, never()).seleccionar(any(), any(), any());
    }

    @Test
    void conConvocatoriaInexistente_da404ErrPri01() throws Exception {
        when(service.seleccionar(eq(admin), eq(convocatoria), any()))
                .thenThrow(new NotFoundException("ERR_PRI_01", "La convocatoria no existe"));

        mockMvc.perform(post(ruta()).with(sesion()).contentType(JSON).content("{\"cantidad\":3}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("ERR_PRI_01"));
    }

    @Test
    void conConflicto_da409ConSuCodigo() throws Exception {
        for (String codigo : new String[] {AdjudicacionErrorCodes.ERR_ADJ_01, AdjudicacionErrorCodes.ERR_ADJ_05}) {
            when(service.seleccionar(eq(admin), eq(convocatoria), any()))
                    .thenThrow(new ConflictException(codigo, "Conflicto"));

            mockMvc.perform(post(ruta()).with(sesion()).contentType(JSON).content("{\"cantidad\":3}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value(codigo));
        }
    }
}
