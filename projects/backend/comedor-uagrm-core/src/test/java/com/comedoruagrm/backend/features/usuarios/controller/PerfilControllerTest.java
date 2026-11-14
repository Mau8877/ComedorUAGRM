package com.comedoruagrm.backend.features.usuarios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.usuarios.dto.ActualizarPerfilRequest;
import com.comedoruagrm.backend.features.usuarios.dto.CarreraResponse;
import com.comedoruagrm.backend.features.usuarios.dto.PerfilPostulanteResponse;
import com.comedoruagrm.backend.features.usuarios.dto.PerfilResponse;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.service.PerfilService;
import com.comedoruagrm.backend.security.AuthErrorCodes;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider;
import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

/** Contrato HTTP de GET y PATCH /api/v1/perfil (CU15) con el PerfilService simulado. */
@SpringBootTest
@AutoConfigureMockMvc
class PerfilControllerTest {

    private static final String RUTA = "/api/v1/perfil";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilService perfilService;

    @MockitoBean
    private UsuarioAutenticableProvider provider;

    private final UUID codigo = UUID.randomUUID();
    private final PerfilResponse perfil = new PerfilResponse(codigo, "Ana", "Perez", "1234567",
            "ana@uagrm.edu.bo", "POSTULANTE", true,
            new PerfilPostulanteResponse("219012345",
                    new CarreraResponse(UUID.randomUUID(), "Ingeniería Informática", "Tecnología"), "70000001"));

    @BeforeEach
    void autenticar() {
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", true)));
    }

    @Test
    void obtener_sinSesion_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void obtener_conSesion_devuelveElPerfilDelUsuarioDelToken() throws Exception {
        when(perfilService.obtener(codigo)).thenReturn(perfil);

        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.ci").value("1234567"))
                .andExpect(jsonPath("$.data.rol").value("POSTULANTE"))
                .andExpect(jsonPath("$.data.postulante.registroUniversitario").value("219012345"))
                .andExpect(jsonPath("$.data.postulante.carrera.nombre").value("Ingeniería Informática"))
                .andExpect(jsonPath("$.data.postulante.telefono").value("70000001"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void obtener_deUnQueNoEsPostulante_traePostulanteNulo() throws Exception {
        when(perfilService.obtener(codigo)).thenReturn(new PerfilResponse(codigo, "Luis", "Rojas", "7654321",
                "luis@uagrm.edu.bo", "ADMINISTRADOR_DUBSS", true, null));

        mockMvc.perform(get(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postulante").doesNotExist());
    }

    @Test
    void actualizar_sinSesion_devuelve401ErrAuth01() throws Exception {
        mockMvc.perform(patch(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"71111111\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_01"));
    }

    @Test
    void actualizar_conSesion_devuelveElPerfilActualizado() throws Exception {
        when(perfilService.actualizar(eq(codigo), any())).thenReturn(
                new ResultadoAuditado<>(perfil, 1L, codigo, 1L, Map.of("campos", java.util.List.of("telefono")), true));

        mockMvc.perform(patch(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"71111111\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil actualizado correctamente"))
                .andExpect(jsonPath("$.data.codigo").value(codigo.toString()));
    }

    @Test
    void actualizar_conCiYOtrosCamposDeMasEnElCuerpo_losIgnora() throws Exception {
        when(perfilService.actualizar(eq(codigo), any())).thenReturn(
                new ResultadoAuditado<>(perfil, 1L, codigo, 1L, Map.of("campos", java.util.List.of()), false));

        mockMvc.perform(patch(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"71111111\",\"ci\":\"999\",\"rol\":\"ADMINISTRADOR_DUBSS\","
                                + "\"nombres\":\"Otro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ci").value("1234567"));

        ArgumentCaptor<ActualizarPerfilRequest> captor = ArgumentCaptor.forClass(ActualizarPerfilRequest.class);
        verify(perfilService).actualizar(eq(codigo), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue())
                .isEqualTo(new ActualizarPerfilRequest(null, "71111111", null));
    }

    @Test
    void actualizar_conUnTelefonoEnBlancoOCorreoMalFormado_devuelve400ErrSys01() throws Exception {
        mockMvc.perform(patch(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
        mockMvc.perform(patch(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_SYS_01"));
    }

    @Test
    void actualizar_conLaContrasenaActualIncorrecta_devuelve400ErrAuth09() throws Exception {
        when(perfilService.actualizar(any(), any())).thenThrow(
                new ValidationException(AuthErrorCodes.ERR_AUTH_09, "La contraseña actual es incorrecta."));

        mockMvc.perform(patch(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nuevo@uagrm.edu.bo\",\"contrasenaActual\":\"mala\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ERR_AUTH_09"));
    }

    @Test
    void actualizar_conUnCorreoDeOtraCuenta_devuelve409ErrUs02() throws Exception {
        when(perfilService.actualizar(any(), any())).thenThrow(new ConflictException(UsuarioErrorCodes.ERR_US_02,
                "Ya existe una cuenta con ese correo electrónico"));

        mockMvc.perform(patch(RUTA).with(jwt().jwt(j -> j.subject(codigo.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"otro@uagrm.edu.bo\",\"contrasenaActual\":\"clave\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ERR_US_02"));
    }
}
