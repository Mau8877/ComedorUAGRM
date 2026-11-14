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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.comedoruagrm.backend.features.notificaciones.enums.PlataformaDispositivo;
import com.comedoruagrm.backend.features.notificaciones.service.DispositivoFcmService;
import com.comedoruagrm.backend.features.notificaciones.service.DispositivoFcmService.DispositivoRegistrado;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de los endpoints de dispositivos (CU-EXTRA-01) con el servicio simulado. */
@SpringBootTest
@AutoConfigureMockMvc
class DispositivoFcmControllerTest {

    private static final String RUTA = "/api/v1/dispositivos-fcm";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispositivoFcmService dispositivoService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID codigo = UUID.randomUUID();

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", true)));
    }

    @Test
    void registrar_sinSesion_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"plataforma\":\"ANDROID\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void registrar_conTokenNuevo_devuelve201ConElCodigoYSinElToken() throws Exception {
        UUID codigoDispositivo = UUID.randomUUID();
        when(dispositivoService.registrar(codigo, "abc", PlataformaDispositivo.ANDROID))
                .thenReturn(new DispositivoRegistrado(codigoDispositivo, PlataformaDispositivo.ANDROID, true));

        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"plataforma\":\"ANDROID\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.codigo").value(codigoDispositivo.toString()))
                .andExpect(jsonPath("$.data.plataforma").value("ANDROID"))
                .andExpect(jsonPath("$.data.token").doesNotExist());
    }

    @Test
    void registrar_conTokenYaRegistrado_devuelve200() throws Exception {
        when(dispositivoService.registrar(codigo, "abc", PlataformaDispositivo.ANDROID))
                .thenReturn(new DispositivoRegistrado(UUID.randomUUID(), PlataformaDispositivo.ANDROID, false));

        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"plataforma\":\"ANDROID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void registrar_conTokenVacio_devuelve400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"  \",\"plataforma\":\"ANDROID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void registrar_sinPlataforma_devuelve400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void registrar_conPlataformaDesconocida_devuelve400() throws Exception {
        mockMvc.perform(post(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\",\"plataforma\":\"WINDOWS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"));
    }

    @Test
    void darDeBaja_sinSesion_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(post(RUTA + "/baja").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"abc\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void darDeBaja_conSesion_devuelve200SinDatosYDelegaConElUsuarioDelToken() throws Exception {
        mockMvc.perform(post(RUTA + "/baja").with(jwt().jwt(j -> j.subject(codigo.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(dispositivoService).darDeBaja(codigo, "abc");
    }

    @Test
    void darDeBaja_conTokenVacio_devuelve400ErrSys01() throws Exception {
        mockMvc.perform(post(RUTA + "/baja").with(jwt().jwt(j -> j.subject(codigo.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }
}
