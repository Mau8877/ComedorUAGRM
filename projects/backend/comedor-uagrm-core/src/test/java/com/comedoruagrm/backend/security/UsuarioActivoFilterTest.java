package com.comedoruagrm.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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

class UsuarioActivoFilterTest {

    private final UsuarioAutenticableProvider provider = mock(UsuarioAutenticableProvider.class);
    private final UsuarioActivoFilter filter = new UsuarioActivoFilter(provider, new SecurityErrorWriter());

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filtrar_conUsuarioActivo_dejaElPrincipalConSuRolYContinua() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", true)));
        conToken(codigo.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/auth/usuario-actual"), response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo(new UsuarioAutenticado(codigo, "POSTULANTE"));
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_POSTULANTE");
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void filtrar_conUsuarioDesactivado_responde403ErrAuth03SinContinuar() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.of(new DatosUsuario(codigo, "POSTULANTE", false)));
        conToken(codigo.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/auth/usuario-actual"), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"status\":\"failed\"").contains("\"error\":\"ERR_AUTH_03\"");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void filtrar_conUnUsuarioQueYaNoExiste_responde401ErrAuth02SinContinuar() throws Exception {
        UUID codigo = UUID.randomUUID();
        when(provider.buscarPorCodigo(codigo)).thenReturn(Optional.empty());
        conToken(codigo.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/auth/usuario-actual"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"error\":\"ERR_AUTH_02\"");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void filtrar_conUnSubjectQueNoEsUnCodigo_responde401ErrAuth01SinConsultar() throws Exception {
        conToken("no-es-un-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/auth/usuario-actual"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"error\":\"ERR_AUTH_01\"");
        assertThat(chain.getRequest()).isNull();
        verifyNoInteractions(provider);
    }

    @Test
    void filtrar_sinAutenticacionPrevia_continuaSinConsultar() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/algo"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(provider);
    }

    @Test
    void filtrar_unaRutaFueraDeLaApiDeNegocio_noConsultaAlProvider() throws Exception {
        conToken(UUID.randomUUID().toString());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/health"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(provider);
    }

    private MockHttpServletRequest request(String metodo, String ruta) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setRequestURI(ruta);
        return request;
    }

    private void conToken(String subject) {
        Jwt jwt = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
