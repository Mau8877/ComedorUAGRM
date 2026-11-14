package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.model.Notificacion;
import com.comedoruagrm.backend.features.notificaciones.repository.NotificacionRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class BandejaNotificacionesServiceTest {

    @Mock
    private NotificacionRepository repositorio;

    @Mock
    private UsuarioService usuarioService;

    private BandejaNotificacionesService service;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        service = new BandejaNotificacionesService(repositorio, usuarioService);
        ana = new Usuario(null, "1234567", "ana@uagrm.edu.bo", "hash", "Ana", "Perez");
        ReflectionTestUtils.setField(ana, "id", 7L);
        ReflectionTestUtils.setField(ana, "codigo", UUID.randomUUID());
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
    }

    private static Notificacion aviso(Usuario usuario, boolean leida) {
        Notificacion n = new Notificacion(usuario, "Titulo", "Mensaje", TipoNotificacion.INFO);
        ReflectionTestUtils.setField(n, "codigo", UUID.randomUUID());
        n.setLeida(leida);
        return n;
    }

    @Test
    void listar_sinSort_ordenaPorFechaDescendenteYMapeaElAviso() {
        Notificacion n = aviso(ana, false);
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        when(repositorio.findByUsuarioId(any(Long.class), pagina.capture()))
                .thenAnswer(i -> new PageImpl<>(List.of(n), i.getArgument(1), 23));

        BandejaNotificacionesService.PaginaNotificaciones resultado = service.listar(ana.getCodigo(), 1, 10, null);

        assertThat(pagina.getValue().getSort().getOrderFor("creadoEn").isDescending()).isTrue();
        assertThat(resultado.datos().get(0).id()).isEqualTo(n.getCodigo());
        assertThat(resultado.datos().get(0).detalle()).isEqualTo("Mensaje");
        assertThat(resultado.meta().totalItems()).isEqualTo(23);
        assertThat(resultado.meta().totalPages()).isEqualTo(3);
    }

    @Test
    void listar_conSortFechaAscendente_funcionaYOtroSortLanzaErrNot02() {
        when(repositorio.findByUsuarioId(any(Long.class), any(Pageable.class)))
                .thenAnswer(i -> new PageImpl<Notificacion>(List.of(), i.getArgument(1), 0));
        service.listar(ana.getCodigo(), 1, 10, "fecha");

        assertThatThrownBy(() -> service.listar(ana.getCodigo(), 1, 10, "titulo"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_NOT_02"));
    }

    @Test
    void marcarLeida_cambiaLaBanderaYEsIdempotente() {
        Notificacion pendiente = aviso(ana, false);
        when(repositorio.findByCodigoAndUsuarioId(pendiente.getCodigo(), 7L)).thenReturn(Optional.of(pendiente));
        service.marcarLeida(ana.getCodigo(), pendiente.getCodigo());
        assertThat(pendiente.isLeida()).isTrue();
        verify(repositorio).save(pendiente);

        Notificacion leida = aviso(ana, true);
        when(repositorio.findByCodigoAndUsuarioId(leida.getCodigo(), 7L)).thenReturn(Optional.of(leida));
        service.marcarLeida(ana.getCodigo(), leida.getCodigo());
        verify(repositorio, never()).save(leida);
    }

    @Test
    void marcarLeida_deUnAvisoAjenoOInexistente_lanza404ErrNot01() {
        UUID ajeno = UUID.randomUUID();
        when(repositorio.findByCodigoAndUsuarioId(ajeno, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarLeida(ana.getCodigo(), ajeno))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_NOT_01"));
    }

    @Test
    void resumir_cuentaLasNoLeidas_yLeerTodoYEliminarOperanSoloSobreElUsuario() {
        when(repositorio.countByUsuarioIdAndLeidaFalse(7L)).thenReturn(4L);

        assertThat(service.resumir(ana.getCodigo()).noLeidas()).isEqualTo(4);
        service.marcarTodasLeidas(ana.getCodigo());
        service.eliminarTodas(ana.getCodigo());

        verify(repositorio).marcarTodasLeidas(7L);
        verify(repositorio).eliminarTodas(7L);
    }
}
