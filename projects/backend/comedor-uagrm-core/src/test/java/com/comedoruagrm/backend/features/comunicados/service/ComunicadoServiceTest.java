package com.comedoruagrm.backend.features.comunicados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.features.comunicados.dto.ComunicadoResponse;
import com.comedoruagrm.backend.features.comunicados.dto.PublicarComunicadoRequest;
import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;
import com.comedoruagrm.backend.features.comunicados.model.Comunicado;
import com.comedoruagrm.backend.features.comunicados.repository.ComunicadoConsultaRepository;
import com.comedoruagrm.backend.features.comunicados.repository.ComunicadoRepository;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class ComunicadoServiceTest {

    @Mock
    private ComunicadoRepository comunicadoRepository;

    @Mock
    private ComunicadoConsultaRepository consulta;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private NotificacionService notificacionService;

    private ComunicadoService service;
    private Usuario admin;
    private final UUID convocatoria = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new ComunicadoService(comunicadoRepository, consulta, usuarioService, notificacionService);
        admin = new Usuario(null, "0000001", "admin@dubss.com", "hash", "Admin", "DUBSS");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "codigo", UUID.randomUUID());
    }

    private static PublicarComunicadoRequest pedido(DestinatariosComunicado tipo, UUID convocatoriaId) {
        return new PublicarComunicadoRequest(" Nuevo horario ", " Desde el lunes el comedor abre a las 11 ", tipo,
                convocatoriaId);
    }

    private void guardaComunicado() {
        when(comunicadoRepository.save(any(Comunicado.class))).thenAnswer(i -> {
            Comunicado c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 9L);
            ReflectionTestUtils.setField(c, "codigo", UUID.randomUUID());
            return c;
        });
    }

    private void assertError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
        verify(comunicadoRepository, never()).save(any());
        verify(notificacionService, never()).notificarUsuarios(anyList(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicar_aTodos_guardaElComunicadoYAvisaACadaDestinatarioSinCorreo() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(consulta.destinatarios(DestinatariosComunicado.TODOS, null)).thenReturn(List.of(7L, 8L, 9L));
        guardaComunicado();
        when(notificacionService.notificarUsuarios(anyList(), eq(EventoNotificacion.COMUNICADO), any(), eq(9L)))
                .thenReturn(3);

        ResultadoAuditado<ComunicadoResponse> resultado =
                service.publicar(admin.getCodigo(), pedido(DestinatariosComunicado.TODOS, null));

        assertThat(resultado.datos().totalDestinatarios()).isEqualTo(3);
        assertThat(resultado.datos().titulo()).isEqualTo("Nuevo horario");
        ArgumentCaptor<Map<String, String>> datos = ArgumentCaptor.forClass(Map.class);
        verify(notificacionService).notificarUsuarios(eq(List.of(7L, 8L, 9L)), eq(EventoNotificacion.COMUNICADO),
                datos.capture(), eq(9L));
        assertThat(datos.getValue()).containsEntry("titulo", "Nuevo horario");
        // La bitacora lleva codigo, grupo y cantidad, no el contenido.
        assertThat(resultado.detalle()).containsEntry("destinatarios", "TODOS").containsEntry("totalDestinatarios", 3)
                .doesNotContainValue("Desde el lunes el comedor abre a las 11");
    }

    @Test
    void publicar_aFinalistas_resuelveLaConvocatoriaYSusFinalistas() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(consulta.buscarConvocatoriaId(convocatoria)).thenReturn(Optional.of(5L));
        when(consulta.destinatarios(DestinatariosComunicado.FINALISTAS, 5L)).thenReturn(List.of(7L));
        guardaComunicado();
        when(notificacionService.notificarUsuarios(anyList(), any(), any(), any())).thenReturn(1);

        ResultadoAuditado<ComunicadoResponse> resultado =
                service.publicar(admin.getCodigo(), pedido(DestinatariosComunicado.FINALISTAS, convocatoria));

        assertThat(resultado.datos().convocatoriaId()).isEqualTo(convocatoria);
        assertThat(resultado.datos().destinatarios()).isEqualTo(DestinatariosComunicado.FINALISTAS);
    }

    @Test
    void publicar_adjudicados_lanzaErrCom01() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);

        assertError(() -> service.publicar(admin.getCodigo(),
                pedido(DestinatariosComunicado.ADJUDICADOS, convocatoria)), "ERR_COM_01");
    }

    @Test
    void publicar_finalistasSinConvocatoria_lanzaErrCom02() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);

        assertError(() -> service.publicar(admin.getCodigo(), pedido(DestinatariosComunicado.FINALISTAS, null)),
                "ERR_COM_02");
    }

    @Test
    void publicar_conUnaConvocatoriaInexistente_lanza404ErrFin02() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(consulta.buscarConvocatoriaId(convocatoria)).thenReturn(Optional.empty());

        assertError(() -> service.publicar(admin.getCodigo(), pedido(DestinatariosComunicado.TODOS, convocatoria)),
                "ERR_FIN_02");
    }

    @Test
    void publicar_aUnGrupoSinDestinatarios_lanzaErrCom01YNoPublicaNada() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(consulta.buscarConvocatoriaId(convocatoria)).thenReturn(Optional.of(5L));
        when(consulta.destinatarios(DestinatariosComunicado.FINALISTAS, 5L)).thenReturn(List.of());

        assertError(() -> service.publicar(admin.getCodigo(),
                pedido(DestinatariosComunicado.FINALISTAS, convocatoria)), "ERR_COM_01");
    }

    @Test
    void listar_armaLaMeta() {
        when(consulta.contar()).thenReturn(25L);
        when(consulta.listar(10, 10L)).thenReturn(List.of());

        ComunicadoService.PaginaComunicados pagina = service.listar(2, 10);

        assertThat(pagina.meta().totalPages()).isEqualTo(3);
        assertThat(pagina.meta().page()).isEqualTo(2);
    }
}
