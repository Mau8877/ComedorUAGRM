package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.notificaciones.dto.NotificacionResponse;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.model.Notificacion;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionLoteRepository.AvisoGuardado;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionRepository;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionRepository.CodigoDePostulacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionLoteService.AvisoParaUsuario;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/**
 * El codigo publico de la postulacion en los avisos (CU04, A6): en la bandeja, en el evento de despacho y en el
 * `data` del push. Sin postulacion no se agrega nada.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionPostulacionCodigoTest {

    @Mock
    private NotificacionRepository repositorio;

    @Mock
    private NotificacionLoteRepository loteRepositorio;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private ApplicationEventPublisher publicador;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @Mock
    private CanalPush canalPush;

    @Mock
    private DispositivoFcmService dispositivoService;

    @Mock
    private CorreoNotificacionService correoService;

    @Mock
    private NotificacionService notificacionService;

    private Usuario ana;
    private final UUID postulacionCodigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        ana = new Usuario(null, "1234567", "ana@uagrm.edu.bo", "hash", "Ana", "Perez");
        ReflectionTestUtils.setField(ana, "id", 7L);
        ReflectionTestUtils.setField(ana, "codigo", UUID.randomUUID());
    }

    private static CodigoDePostulacion codigo(long id, UUID codigo) {
        return new CodigoDePostulacion() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public UUID getCodigo() {
                return codigo;
            }
        };
    }

    private Notificacion aviso(Long postulacionId) {
        Notificacion n = new Notificacion(ana, "Titulo", "Mensaje", TipoNotificacion.INFO, postulacionId, null);
        ReflectionTestUtils.setField(n, "codigo", UUID.randomUUID());
        return n;
    }

    // ---- la bandeja ----

    @Test
    void bandeja_unAvisoConPostulacionDevuelveSuCodigo_yUnoSinPostulacionDevuelveNulo_conUnaSolaConsulta() {
        BandejaNotificacionesService bandeja = new BandejaNotificacionesService(repositorio, usuarioService);
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(repositorio.findByUsuarioId(any(Long.class), any(Pageable.class))).thenAnswer(
                i -> new PageImpl<>(List.of(aviso(70L), aviso(null), aviso(71L), aviso(70L)), i.getArgument(1), 4));
        UUID otra = UUID.randomUUID();
        when(repositorio.codigosDePostulaciones(anyCollection()))
                .thenReturn(List.of(codigo(70L, postulacionCodigo), codigo(71L, otra)));

        List<NotificacionResponse> datos = bandeja.listar(ana.getCodigo(), 1, 10, null).datos();

        assertThat(datos).extracting(NotificacionResponse::postulacionCodigo)
                .containsExactly(postulacionCodigo, null, otra, postulacionCodigo);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> ids = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(repositorio, times(1)).codigosDePostulaciones(ids.capture());
        assertThat(Set.copyOf(ids.getValue())).containsExactlyInAnyOrder(70L, 71L);
    }

    @Test
    void bandeja_sinAvisosConPostulacion_noHaceLaConsultaDeCodigos() {
        BandejaNotificacionesService bandeja = new BandejaNotificacionesService(repositorio, usuarioService);
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(repositorio.findByUsuarioId(any(Long.class), any(Pageable.class)))
                .thenAnswer(i -> new PageImpl<>(List.of(aviso(null)), i.getArgument(1), 1));

        List<NotificacionResponse> datos = bandeja.listar(ana.getCodigo(), 1, 10, null).datos();

        assertThat(datos.get(0).postulacionCodigo()).isNull();
        verify(repositorio, never()).codigosDePostulaciones(anyCollection());
    }

    // ---- el evento de despacho ----

    @Test
    void notificar_conPostulacion_publicaElEventoConSuCodigo_ySinPostulacionNoLoBusca() {
        NotificacionService service = new NotificacionService(repositorio, new PlantillaNotificacion(), publicador,
                usuarioService, entityManager);
        when(repositorio.save(any(Notificacion.class))).thenAnswer(i -> {
            Notificacion n = i.getArgument(0);
            ReflectionTestUtils.setField(n, "id", 99L);
            ReflectionTestUtils.setField(n, "codigo", UUID.randomUUID());
            return n;
        });
        when(repositorio.codigoDePostulacion(70L)).thenReturn(Optional.of(postulacionCodigo));

        service.notificar(ana, EventoNotificacion.PRUEBA, Map.of("nombres", "Ana"), 70L, null);
        service.notificar(ana, EventoNotificacion.PRUEBA, Map.of("nombres", "Ana"));

        ArgumentCaptor<NotificacionGuardada> evento = ArgumentCaptor.forClass(NotificacionGuardada.class);
        verify(publicador, times(2)).publishEvent(evento.capture());
        assertThat(evento.getAllValues().get(0).postulacionCodigo()).isEqualTo(postulacionCodigo);
        assertThat(evento.getAllValues().get(1).postulacionCodigo()).isNull();
        verify(repositorio, times(1)).codigoDePostulacion(any());
    }

    @Test
    void notificarLote_resuelveLosCodigosEnUnaSolaConsultaPorTramo() {
        NotificacionLoteService service = new NotificacionLoteService(loteRepositorio, new PlantillaNotificacion(),
                publicador);
        when(loteRepositorio.insertar(anyList())).thenAnswer(i -> {
            List<NotificacionLoteRepository.NuevoAviso> nuevos = i.getArgument(0);
            List<AvisoGuardado> filas = new ArrayList<>();
            for (var n : nuevos) {
                filas.add(new AvisoGuardado(n.usuarioId() * 10, UUID.randomUUID(), n.usuarioId()));
            }
            return filas;
        });
        when(loteRepositorio.codigosDePostulaciones(anyCollection())).thenReturn(Map.of(1001L, postulacionCodigo));
        Map<String, String> datos = Map.of("nombres", "N", "convocatoria", "Beca");

        service.notificarLote(List.of(
                new AvisoParaUsuario(1, "a@x.test", EventoNotificacion.BECA_ADJUDICADA, datos, 1001L),
                new AvisoParaUsuario(2, "b@x.test", EventoNotificacion.BECA_ADJUDICADA, datos, null)));

        ArgumentCaptor<LoteNotificacionesGuardadas> lote = ArgumentCaptor.forClass(LoteNotificacionesGuardadas.class);
        verify(publicador).publishEvent(lote.capture());
        assertThat(lote.getValue().avisos()).extracting(NotificacionGuardada::postulacionCodigo)
                .containsExactly(postulacionCodigo, null);
        verify(loteRepositorio, times(1)).codigosDePostulaciones(anyCollection());
    }

    // ---- el push ----

    private DespachoNotificaciones despacho() {
        return new DespachoNotificaciones(canalPush, dispositivoService, correoService, notificacionService,
                new Reintentos(3, new long[] {0}));
    }

    @Test
    void push_conPostulacion_llevaSuCodigoEnElData_ySinPostulacionNoLoAgrega() {
        when(dispositivoService.tokensDe(7L)).thenReturn(List.of("t1"));
        when(canalPush.enviar(anyList(), any(MensajePush.class))).thenReturn(new ResultadoPush(1, List.of(), List.of()));
        NotificacionGuardada con = new NotificacionGuardada(1L, UUID.randomUUID(), 7L, null, "T", "M",
                EventoNotificacion.POSTULACION_DEVUELTA, postulacionCodigo);
        NotificacionGuardada sin = new NotificacionGuardada(2L, UUID.randomUUID(), 7L, null, "T", "M",
                EventoNotificacion.COMUNICADO, null);

        despacho().despachar(con);
        despacho().despachar(sin);

        ArgumentCaptor<MensajePush> mensaje = ArgumentCaptor.forClass(MensajePush.class);
        verify(canalPush, times(2)).enviar(anyList(), mensaje.capture());
        assertThat(mensaje.getAllValues().get(0).datos()).containsEntry("postulacionCodigo", postulacionCodigo.toString())
                .containsEntry("codigoNotificacion", con.notificacionCodigo().toString())
                .containsEntry("evento", "POSTULACION_DEVUELTA");
        assertThat(mensaje.getAllValues().get(1).datos()).doesNotContainKey("postulacionCodigo")
                .containsKeys("codigoNotificacion", "evento");
    }
}
