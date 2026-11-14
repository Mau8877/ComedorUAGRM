package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.model.Notificacion;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private ApplicationEventPublisher publicador;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    private NotificacionService service;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        service = new NotificacionService(notificacionRepository, new PlantillaNotificacion(), publicador,
                usuarioService, entityManager);
        ana = new Usuario(null, "1234567", "ana@uagrm.edu.bo", "hash", "Ana", "Perez");
        ReflectionTestUtils.setField(ana, "id", 7L);
        ReflectionTestUtils.setField(ana, "codigo", UUID.randomUUID());
    }

    @Test
    void notificar_guardaElAvisoRenderizadoYPublicaElEventoDeDespacho() {
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocacion -> {
            Notificacion n = invocacion.getArgument(0);
            ReflectionTestUtils.setField(n, "id", 99L);
            ReflectionTestUtils.setField(n, "codigo", UUID.randomUUID());
            return n;
        });

        service.notificar(ana, EventoNotificacion.PRUEBA, Map.of("nombres", "Ana"));

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getUsuario()).isSameAs(ana);
        assertThat(guardada.getValue().getTipo()).isEqualTo(TipoNotificacion.INFO);
        assertThat(guardada.getValue().getMensaje()).contains("Hola Ana,");

        ArgumentCaptor<NotificacionGuardada> evento = ArgumentCaptor.forClass(NotificacionGuardada.class);
        verify(publicador).publishEvent(evento.capture());
        assertThat(evento.getValue().notificacionId()).isEqualTo(99L);
        assertThat(evento.getValue().usuarioId()).isEqualTo(7L);
        assertThat(evento.getValue().correo()).isEqualTo("ana@uagrm.edu.bo");
        assertThat(evento.getValue().evento()).isEqualTo(EventoNotificacion.PRUEBA);
    }

    @Test
    void notificar_conUnDatoQueFalta_noGuardaNiPublicaNiLanza() {
        service.notificar(ana, EventoNotificacion.PRUEBA, Map.of());

        verify(notificacionRepository, never()).save(any(Notificacion.class));
        verify(publicador, never()).publishEvent(any(Object.class));
    }

    @Test
    void notificar_conContexto_guardaLaPostulacionYDevuelveTrue() {
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(i -> {
            Notificacion n = i.getArgument(0);
            ReflectionTestUtils.setField(n, "id", 5L);
            ReflectionTestUtils.setField(n, "codigo", UUID.randomUUID());
            return n;
        });

        boolean registrada = service.notificar(ana, EventoNotificacion.PRUEBA, Map.of("nombres", "Ana"), 42L, null);

        assertThat(registrada).isTrue();
        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getPostulacionId()).isEqualTo(42L);
        assertThat(guardada.getValue().getComunicadoId()).isNull();
    }

    @Test
    void notificar_conUnDatoQueFalta_devuelveFalse() {
        assertThat(service.notificar(ana, EventoNotificacion.PRUEBA, Map.of())).isFalse();
    }

    @Test
    void notificarUsuarios_guardaUnAvisoPorUsuarioLigadoAlComunicadoSinCorreo() {
        when(entityManager.getReference(Usuario.class, 7L)).thenReturn(ana);
        when(entityManager.getReference(Usuario.class, 8L)).thenReturn(ana);
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(i -> {
            Notificacion n = i.getArgument(0);
            ReflectionTestUtils.setField(n, "id", 1L);
            ReflectionTestUtils.setField(n, "codigo", UUID.randomUUID());
            return n;
        });

        int guardados = service.notificarUsuarios(java.util.List.of(7L, 8L), EventoNotificacion.COMUNICADO,
                Map.of("titulo", "Hola a todos", "contenido", "Texto del comunicado"), 9L);

        assertThat(guardados).isEqualTo(2);
        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, org.mockito.Mockito.times(2)).save(guardada.capture());
        assertThat(guardada.getAllValues()).allSatisfy(n -> {
            assertThat(n.getComunicadoId()).isEqualTo(9L);
            assertThat(n.getTitulo()).isEqualTo("Hola a todos");
            assertThat(n.getMensaje()).isEqualTo("Texto del comunicado");
        });
        ArgumentCaptor<NotificacionGuardada> evento = ArgumentCaptor.forClass(NotificacionGuardada.class);
        verify(publicador, org.mockito.Mockito.times(2)).publishEvent(evento.capture());
        assertThat(evento.getAllValues()).allSatisfy(e -> {
            assertThat(e.correo()).isNull();
            assertThat(e.evento().enviaCorreo()).isFalse();
        });
    }

    @Test
    void notificarUsuarios_conUnEventoConCorreo_seRechaza() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.notificarUsuarios(
                java.util.List.of(7L), EventoNotificacion.PRUEBA, Map.of("nombres", "x"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enviarPrueba_notificaAlPropioUsuarioConSuNombre() {
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        service.enviarPrueba(ana.getCodigo());

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getUsuario()).isSameAs(ana);
        assertThat(guardada.getValue().getMensaje()).contains("Hola Ana,");
    }

    @Test
    void marcarEnviada_llenaEnviadaEn() {
        Notificacion notificacion = new Notificacion(ana, "t", "m", TipoNotificacion.INFO);
        when(notificacionRepository.findById(99L)).thenReturn(Optional.of(notificacion));

        service.marcarEnviada(99L);

        assertThat(notificacion.getEnviadaEn()).isNotNull();
        verify(notificacionRepository).save(notificacion);
    }
}
