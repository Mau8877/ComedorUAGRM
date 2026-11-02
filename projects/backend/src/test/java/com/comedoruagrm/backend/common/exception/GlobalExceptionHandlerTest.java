package com.comedoruagrm.backend.common.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Las excepciones del framework devuelven su codigo HTTP y no caen en el 500 generico. */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    @BeforeEach
    void usuarioActivo() {
        when(provider.buscarPorFirebaseUid(any()))
                .thenReturn(Optional.of(new DatosUsuario(UUID.randomUUID(), "POSTULANTE", true)));
    }

    @Test
    void get_aUnaRutaInexistente_devuelve404ErrSys03() throws Exception {
        mockMvc.perform(get("/api/v1/no-existe-esta-ruta")
                        .with(jwt().jwt(j -> j.subject("uid-x").claim("email", "x@uagrm.edu.bo"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("ERR_SYS_03"));
    }

    @Test
    void put_aUnaRutaQueSoloAdmiteGet_devuelve405ErrSys04() throws Exception {
        mockMvc.perform(put("/api/v1/carreras")
                        .with(jwt().jwt(j -> j.subject("uid-x").claim("email", "x@uagrm.edu.bo"))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("ERR_SYS_04"));
    }

    @Test
    void get_conUnParametroConTipoInvalido_devuelve400ErrSys05() throws Exception {
        mockMvc.perform(get("/api/v1/carreras").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }

    @Test
    void post_conUnJsonIlegible_devuelve400ErrSys05() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .with(jwt().jwt(j -> j.subject("uid-x").claim("email", "x@uagrm.edu.bo")))
                        .contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_05"));
    }
}
