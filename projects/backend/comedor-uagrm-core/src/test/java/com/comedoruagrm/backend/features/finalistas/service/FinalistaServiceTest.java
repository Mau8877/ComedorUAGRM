package com.comedoruagrm.backend.features.finalistas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import com.comedoruagrm.backend.features.finalistas.dto.FinalistaResponse;
import com.comedoruagrm.backend.features.finalistas.dto.NotificarFinalistasRequest;
import com.comedoruagrm.backend.features.finalistas.dto.RegistrarEntregaRequest;
import com.comedoruagrm.backend.features.finalistas.dto.ResultadoNotificacionResponse;
import com.comedoruagrm.backend.features.finalistas.model.Finalista;
import com.comedoruagrm.backend.features.finalistas.repository.FinalistaConsultaRepository;
import com.comedoruagrm.backend.features.finalistas.repository.FinalistaRepository;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class FinalistaServiceTest {

    private static final LocalDate HOY = LocalDate.now(FinalistaConsultaRepository.ZONA);

    @Mock
    private FinalistaRepository finalistaRepository;

    @Mock
    private FinalistaConsultaRepository consulta;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private NotificacionService notificacionService;

    private FinalistaService service;
    private Usuario admin;
    private Usuario ana;
    private Finalista finalista;
    private final UUID finalistaCodigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new FinalistaService(finalistaRepository, consulta, usuarioService, notificacionService);
        admin = usuario(1L, "Admin");
        ana = usuario(7L, "Ana");
        finalista = new Finalista(70L, 1);
        ReflectionTestUtils.setField(finalista, "id", 10L);
        ReflectionTestUtils.setField(finalista, "codigo", finalistaCodigo);
    }

    private static Usuario usuario(long id, String nombres) {
        Usuario u = new Usuario(null, "1234" + id, nombres + "@uagrm.edu.bo", "hash", nombres, "Perez");
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "codigo", UUID.randomUUID());
        return u;
    }

    private NotificarFinalistasRequest pedido(LocalDate plazo, UUID... codigos) {
        return new NotificarFinalistasRequest(List.of(codigos), plazo, "DUBSS oficina 3, 8:00 a 12:00",
                "CI y certificado");
    }

    private static void assertCodigo(Throwable e, String codigo) {
        assertThat(e).isInstanceOfSatisfying(BusinessException.class,
                b -> assertThat(b.getErrorCode()).isEqualTo(codigo));
    }

    // ---- listar ----

    @Test
    void listar_conUnaConvocatoriaInexistente_lanza404ErrFin02() {
        UUID convocatoria = UUID.randomUUID();
        when(consulta.existeConvocatoria(convocatoria)).thenReturn(false);

        assertThatThrownBy(() -> service.listar(convocatoria, null, 1, 20)).satisfies(e -> assertCodigo(e, "ERR_FIN_02"));
    }

    @Test
    void listar_armaLaMetaYEscapaLaBusqueda() {
        UUID convocatoria = UUID.randomUUID();
        when(consulta.existeConvocatoria(convocatoria)).thenReturn(true);
        when(consulta.contar(convocatoria, "%100\\%%")).thenReturn(45L);
        when(consulta.buscar(eq(convocatoria), eq("%100\\%%"), eq(20), eq(20L))).thenReturn(List.of());

        FinalistaService.PaginaFinalistas pagina = service.listar(convocatoria, " 100% ", 2, 20);

        assertThat(pagina.meta().totalItems()).isEqualTo(45);
        assertThat(pagina.meta().totalPages()).isEqualTo(3);
    }

    @Test
    void listar_conPaginaCero_lanzaErrSys01() {
        assertThatThrownBy(() -> service.listar(UUID.randomUUID(), null, 0, 20))
                .satisfies(e -> assertCodigo(e, "ERR_SYS_01"));
    }

    // ---- notificar ----

    @Test
    void notificar_conPlazoDeHoy_lanzaErrFin04YNoEnviaNada() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);

        assertThatThrownBy(() -> service.notificar(admin.getCodigo(), pedido(HOY, finalistaCodigo)))
                .satisfies(e -> assertCodigo(e, "ERR_FIN_04"));
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }

    @Test
    void notificar_conUnFinalistaInexistente_lanza404ErrFin01() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(finalistaRepository.findByCodigoIn(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.notificar(admin.getCodigo(), pedido(HOY.plusDays(5), finalistaCodigo)))
                .satisfies(e -> assertCodigo(e, "ERR_FIN_01"));
        verify(notificacionService, never()).notificar(any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void notificar_avisaAlFinalistaLoMarcaYAnotaLaBitacoraSinDatosPersonales() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(finalistaRepository.findByCodigoIn(any())).thenReturn(List.of(finalista));
        when(consulta.codigoUsuarioDePostulacion(70L)).thenReturn(Optional.of(ana.getCodigo()));
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(notificacionService.notificar(eq(ana), eq(EventoNotificacion.FINALISTA_NOTIFICADO), any(), eq(70L),
                any())).thenReturn(true);
        LocalDate plazo = HOY.plusDays(7);

        ResultadoAuditado<ResultadoNotificacionResponse> resultado =
                service.notificar(admin.getCodigo(), pedido(plazo, finalistaCodigo, finalistaCodigo));

        assertThat(resultado.datos()).isEqualTo(new ResultadoNotificacionResponse(1, 1, 0));
        assertThat(finalista.getPlazoEntrega()).isEqualTo(plazo);
        assertThat(finalista.getNotificadoEn()).isNotNull();
        ArgumentCaptor<Map<String, String>> datos = ArgumentCaptor.forClass(Map.class);
        verify(notificacionService).notificar(eq(ana), eq(EventoNotificacion.FINALISTA_NOTIFICADO), datos.capture(),
                eq(70L), any());
        assertThat(datos.getValue()).containsEntry("nombres", "Ana").containsEntry("documentos", "CI y certificado");
        assertThat(resultado.detalle().toString()).contains(finalistaCodigo.toString())
                .doesNotContain("Ana").doesNotContain("@uagrm").doesNotContain(ana.getCi());
        assertThat(resultado.actorId()).isEqualTo(1L);
    }

    @Test
    void notificar_siElAvisoNoSePudoRegistrar_cuentaUnFallidoYNoLoMarcaNotificado() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(finalistaRepository.findByCodigoIn(any())).thenReturn(List.of(finalista));
        when(consulta.codigoUsuarioDePostulacion(70L)).thenReturn(Optional.of(ana.getCodigo()));
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(notificacionService.notificar(any(), any(), any(), anyLong(), any())).thenReturn(false);

        ResultadoAuditado<ResultadoNotificacionResponse> resultado =
                service.notificar(admin.getCodigo(), pedido(HOY.plusDays(3), finalistaCodigo));

        assertThat(resultado.datos()).isEqualTo(new ResultadoNotificacionResponse(1, 0, 1));
        assertThat(finalista.getNotificadoEn()).isNull();
    }

    // ---- registrarEntrega ----

    private void preparaEntrega() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(finalistaRepository.findByCodigo(finalistaCodigo)).thenReturn(Optional.of(finalista));
        when(consulta.buscarUno(finalistaCodigo)).thenReturn(Optional.of(new FinalistaResponse(finalistaCodigo, 1,
                "Ana", "Perez", "123", null, "ENVIADA", null, null, HOY, "SIN_AUDITAR", null, "calificada")));
    }

    @Test
    void registrarEntrega_sinFecha_usaHoyYNoDejaFechaAnterior() {
        preparaEntrega();

        ResultadoAuditado<FinalistaResponse> resultado = service.registrarEntrega(admin.getCodigo(), finalistaCodigo,
                new RegistrarEntregaRequest(null, " Falta una fotocopia "));

        assertThat(finalista.getDocumentacionRecibidaEn())
                .isEqualTo(HOY.atStartOfDay(FinalistaConsultaRepository.ZONA).toInstant());
        assertThat(resultado.detalle()).containsEntry("fechaRecepcion", HOY.toString())
                .containsEntry("observaciones", "Falta una fotocopia").doesNotContainKey("fechaAnterior");
    }

    @Test
    void registrarEntrega_corrigiendoUnaFecha_dejaLaAnteriorEnLaBitacora() {
        preparaEntrega();
        LocalDate antes = HOY.minusDays(3);
        finalista.setDocumentacionRecibidaEn(antes.atStartOfDay(FinalistaConsultaRepository.ZONA).toInstant());

        ResultadoAuditado<FinalistaResponse> resultado = service.registrarEntrega(admin.getCodigo(), finalistaCodigo,
                new RegistrarEntregaRequest(HOY.minusDays(1), null));

        assertThat(resultado.detalle()).containsEntry("fechaAnterior", antes.toString())
                .containsEntry("fechaRecepcion", HOY.minusDays(1).toString()).doesNotContainKey("observaciones");
    }

    @Test
    void registrarEntrega_conFechaFutura_lanzaErrFin03YNoGuarda() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(finalistaRepository.findByCodigo(finalistaCodigo)).thenReturn(Optional.of(finalista));

        assertThatThrownBy(() -> service.registrarEntrega(admin.getCodigo(), finalistaCodigo,
                new RegistrarEntregaRequest(HOY.plusDays(1), null))).satisfies(e -> assertCodigo(e, "ERR_FIN_03"));
        verify(finalistaRepository, never()).saveAndFlush(any());
        assertThat(finalista.getDocumentacionRecibidaEn()).isNull();
    }

    @Test
    void registrarEntrega_conUnFinalistaInexistente_lanza404ErrFin01() {
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(finalistaRepository.findByCodigo(finalistaCodigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarEntrega(admin.getCodigo(), finalistaCodigo,
                new RegistrarEntregaRequest(null, null))).satisfies(e -> assertCodigo(e, "ERR_FIN_01"));
    }
}
