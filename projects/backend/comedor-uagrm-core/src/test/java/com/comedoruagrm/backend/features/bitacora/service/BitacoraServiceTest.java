package com.comedoruagrm.backend.features.bitacora.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.bitacora.model.Bitacora;
import com.comedoruagrm.backend.features.bitacora.repository.BitacoraRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;

import jakarta.persistence.EntityManager;

class BitacoraServiceTest {

    private final BitacoraRepository repository = mock(BitacoraRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final BitacoraService service = new BitacoraService(repository, entityManager);

    @Test
    void registrar_conElActorPorId_guardaUnaReferenciaAlUsuarioSinConsultarla() {
        Usuario referencia = mock(Usuario.class);
        when(entityManager.getReference(Usuario.class, 9L)).thenReturn(referencia);

        service.registrar(9L, UUID.randomUUID(), BitacoraAcciones.INICIO_SESION, "usuario", 9L,
                Map.of("rol", "POSTULANTE"), "203.0.113.10");

        ArgumentCaptor<Bitacora> captor = ArgumentCaptor.forClass(Bitacora.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(referencia);
        assertThat(captor.getValue().getAccion()).isEqualTo("INICIO_SESION");
        assertThat(captor.getValue().getEntidadId()).isEqualTo(9L);
    }

    @Test
    void registrar_conUnEventoCompleto_guardaLaBitacoraConLosDatosRecibidos() {
        Usuario usuario = mock(Usuario.class);

        service.registrar(usuario, BitacoraAcciones.INICIO_SESION, "usuario", 7L,
                Map.of("canal", "web"), "203.0.113.10");

        ArgumentCaptor<Bitacora> captor = ArgumentCaptor.forClass(Bitacora.class);
        verify(repository).save(captor.capture());
        Bitacora guardada = captor.getValue();
        assertThat(guardada.getUsuario()).isSameAs(usuario);
        assertThat(guardada.getAccion()).isEqualTo("INICIO_SESION");
        assertThat(guardada.getEntidad()).isEqualTo("usuario");
        assertThat(guardada.getEntidadId()).isEqualTo(7L);
        assertThat(guardada.getDetalle()).containsEntry("canal", "web");
        assertThat(guardada.getIp().getHostAddress()).isEqualTo("203.0.113.10");
    }

    @Test
    void registrar_conUnaIpQueNoEsLiteral_guardaElEventoSinIp() {
        service.registrar(mock(Usuario.class), BitacoraAcciones.REGISTRO_USUARIO, "usuario", 1L,
                null, "no-es-una-ip.example.com");

        ArgumentCaptor<Bitacora> captor = ArgumentCaptor.forClass(Bitacora.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIp()).isNull();
    }
}
