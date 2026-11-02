package com.comedoruagrm.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.comedoruagrm.backend.security.UsuarioAutenticableProvider.DatosUsuario;

class FirebaseUsuarioFilterTest {

    private final UsuarioAutenticableProvider provider = mock(UsuarioAutenticableProvider.class);
    private final FirebaseUsuarioFilter filter = new FirebaseUsuarioFilter(provider, new SecurityErrorWriter());

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filtrar_conUsuarioActivo_dejaElPrincipalConSuRolYContinua() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(provider.buscarPorFirebaseUid("uid-1")).thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", true)));
        conToken("uid-1", "ana@uagrm.edu.bo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/auth/usuario-actual"), response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo(new UsuarioAutenticado(codigo, "POSTULANTE", "uid-1"));
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_POSTULANTE");
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void filtrar_conUsuarioDesactivado_responde403ErrAuth03SinContinuar() throws Exception {
        when(provider.buscarPorFirebaseUid("uid-2"))
                .thenReturn(Optional.of(new DatosUsuario(UUID.randomUUID(), "POSTULANTE", false)));
        conToken("uid-2", "baja@uagrm.edu.bo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/auth/usuario-actual"), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"status\":\"failed\"").contains("\"error\":\"ERR_AUTH_03\"");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void filtrar_sinCuentaEnUnaRutaNormal_responde401ErrAuth02SinContinuar() throws Exception {
        when(provider.buscarPorFirebaseUid("uid-3")).thenReturn(Optional.empty());
        conToken("uid-3", "nuevo@uagrm.edu.bo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("POST", "/api/v1/auth/sesion"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"error\":\"ERR_AUTH_02\"");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void filtrar_sinCuentaEnElRegistro_dejaPasarConElPrincipalSinRegistro() throws Exception {
        when(provider.buscarPorFirebaseUid("uid-4")).thenReturn(Optional.empty());
        conToken("uid-4", "nuevo@uagrm.edu.bo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("POST", "/api/v1/auth/registro"), response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo(new FirebaseSinRegistro("uid-4", "nuevo@uagrm.edu.bo"));
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void filtrar_elListadoPublicoDeCarreras_noConsultaAlProvider() throws Exception {
        conToken("uid-5", "x@uagrm.edu.bo");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/carreras"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        org.mockito.Mockito.verifyNoInteractions(provider);
    }

    @Test
    void filtrar_sinAutenticacionPrevia_continuaSinConsultar() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/algo"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        org.mockito.Mockito.verifyNoInteractions(provider);
    }

    private MockHttpServletRequest request(String metodo, String ruta) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setRequestURI(ruta);
        return request;
    }

    private void conToken(String uid, String correo) {
        Jwt jwt = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "RS256")
                .subject(uid)
                .claim("email", correo)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
