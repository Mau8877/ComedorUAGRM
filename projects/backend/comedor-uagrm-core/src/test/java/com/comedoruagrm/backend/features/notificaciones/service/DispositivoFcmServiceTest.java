package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.notificaciones.enums.PlataformaDispositivo;
import com.comedoruagrm.backend.features.notificaciones.model.DispositivoFcm;
import com.comedoruagrm.backend.features.notificaciones.repository.DispositivoFcmRepository;
import com.comedoruagrm.backend.features.notificaciones.service.DispositivoFcmService.DispositivoRegistrado;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class DispositivoFcmServiceTest {

    @Mock
    private DispositivoFcmRepository dispositivoRepository;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private DispositivoFcmService service;

    private Usuario ana;
    private Usuario beto;

    @BeforeEach
    void usuarios() {
        ana = usuario(1L);
        beto = usuario(2L);
    }

    @Test
    void registrar_conTokenNuevo_loCreaAsociadoAlUsuario() {
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(dispositivoRepository.findByToken("token")).thenReturn(Optional.empty());
        when(dispositivoRepository.save(any(DispositivoFcm.class))).thenAnswer(i -> i.getArgument(0));

        DispositivoRegistrado resultado = service.registrar(ana.getCodigo(), "token", PlataformaDispositivo.ANDROID);

        assertThat(resultado.creado()).isTrue();
        assertThat(resultado.plataforma()).isEqualTo(PlataformaDispositivo.ANDROID);
        verify(dispositivoRepository).save(any(DispositivoFcm.class));
    }

    @Test
    void registrar_conTokenYaDeEsteUsuario_noDuplicaNiGuarda() {
        DispositivoFcm existente = new DispositivoFcm(ana, "token", PlataformaDispositivo.ANDROID);
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(dispositivoRepository.findByToken("token")).thenReturn(Optional.of(existente));

        DispositivoRegistrado resultado = service.registrar(ana.getCodigo(), "token", PlataformaDispositivo.ANDROID);

        assertThat(resultado.creado()).isFalse();
        verify(dispositivoRepository, never()).save(any(DispositivoFcm.class));
    }

    @Test
    void registrar_conTokenDeOtraCuenta_loReasignaAlUsuarioActual() {
        DispositivoFcm deBeto = new DispositivoFcm(beto, "token", PlataformaDispositivo.ANDROID);
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(dispositivoRepository.findByToken("token")).thenReturn(Optional.of(deBeto));

        DispositivoRegistrado resultado = service.registrar(ana.getCodigo(), "token", PlataformaDispositivo.ANDROID);

        assertThat(resultado.creado()).isFalse();
        assertThat(deBeto.getUsuario()).isSameAs(ana);
        verify(dispositivoRepository).save(deBeto);
    }

    @Test
    void darDeBaja_conTokenDelUsuario_loEliminaLogicamente() {
        DispositivoFcm dispositivo = new DispositivoFcm(ana, "token", PlataformaDispositivo.ANDROID);
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(dispositivoRepository.findByToken("token")).thenReturn(Optional.of(dispositivo));

        service.darDeBaja(ana.getCodigo(), "token");

        assertThat(dispositivo.getEliminadoEn()).isNotNull();
        verify(dispositivoRepository).save(dispositivo);
    }

    @Test
    void darDeBaja_conTokenDeOtraCuenta_noHaceNada() {
        DispositivoFcm deBeto = new DispositivoFcm(beto, "token", PlataformaDispositivo.ANDROID);
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(dispositivoRepository.findByToken("token")).thenReturn(Optional.of(deBeto));

        service.darDeBaja(ana.getCodigo(), "token");

        assertThat(deBeto.getEliminadoEn()).isNull();
        verify(dispositivoRepository, never()).save(any(DispositivoFcm.class));
    }

    @Test
    void darDeBaja_conTokenDesconocido_esIdempotente() {
        when(usuarioService.obtenerPorCodigo(ana.getCodigo())).thenReturn(ana);
        when(dispositivoRepository.findByToken("nada")).thenReturn(Optional.empty());

        service.darDeBaja(ana.getCodigo(), "nada");

        verify(dispositivoRepository, never()).save(any(DispositivoFcm.class));
    }

    @Test
    void tokensDe_devuelveSoloLosTokensDelUsuario() {
        when(dispositivoRepository.findAllByUsuarioId(1L)).thenReturn(List.of(
                new DispositivoFcm(ana, "t1", PlataformaDispositivo.ANDROID),
                new DispositivoFcm(ana, "t2", PlataformaDispositivo.ANDROID)));

        assertThat(service.tokensDe(1L)).containsExactly("t1", "t2");
    }

    @Test
    void tokensDe_conVariosUsuarios_agrupaPorUsuarioConUnaSolaConsulta() {
        when(dispositivoRepository.findAllByUsuarioIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(
                new DispositivoFcm(ana, "a1", PlataformaDispositivo.ANDROID),
                new DispositivoFcm(ana, "a2", PlataformaDispositivo.ANDROID),
                new DispositivoFcm(beto, "b1", PlataformaDispositivo.ANDROID)));

        java.util.Map<Long, List<String>> tokens = service.tokensDe(List.of(1L, 2L, 3L));

        assertThat(tokens).containsOnlyKeys(1L, 2L);
        assertThat(tokens.get(1L)).containsExactly("a1", "a2");
        assertThat(tokens.get(2L)).containsExactly("b1");
        verify(dispositivoRepository, org.mockito.Mockito.times(1)).findAllByUsuarioIdIn(anyCollection());
    }

    @Test
    void tokensDe_conUnaColeccionVacia_noConsultaNada() {
        assertThat(service.tokensDe(List.<Long>of())).isEmpty();

        verify(dispositivoRepository, never()).findAllByUsuarioIdIn(anyCollection());
    }

    @Test
    void eliminarInvalidos_eliminaLogicamenteLosTokensIndicados() {
        DispositivoFcm a = new DispositivoFcm(ana, "malo-1", PlataformaDispositivo.ANDROID);
        DispositivoFcm b = new DispositivoFcm(ana, "malo-2", PlataformaDispositivo.ANDROID);
        when(dispositivoRepository.findAllByTokenIn(List.of("malo-1", "malo-2"))).thenReturn(List.of(a, b));

        service.eliminarInvalidos(List.of("malo-1", "malo-2"));

        assertThat(a.getEliminadoEn()).isNotNull();
        assertThat(b.getEliminadoEn()).isNotNull();
        verify(dispositivoRepository).saveAll(List.of(a, b));
    }

    @Test
    void eliminarInvalidos_sinTokens_noConsultaNada() {
        service.eliminarInvalidos(List.of());

        verify(dispositivoRepository, never()).findAllByTokenIn(anyList());
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario(null, "ci-" + id, "u" + id + "@uagrm.edu.bo", "hash", "Nombre", "Apellido");
        ReflectionTestUtils.setField(usuario, "id", id);
        ReflectionTestUtils.setField(usuario, "codigo", UUID.randomUUID());
        return usuario;
    }
}
