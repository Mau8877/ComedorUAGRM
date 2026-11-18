package com.comedoruagrm.backend.features.panel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.prepost.PreAuthorize;

import com.comedoruagrm.backend.features.panel.dto.PanelAdministradorResponse;
import com.comedoruagrm.backend.features.panel.dto.PanelComedorResponse;
import com.comedoruagrm.backend.features.panel.repository.PanelRepository;
import com.comedoruagrm.backend.features.panel.repository.PanelRepository.AsistenciaFecha;
import com.comedoruagrm.backend.features.panel.repository.PanelRepository.ConvocatoriaAbiertaFila;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Resúmenes de las pantallas de inicio con el repositorio simulado. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PanelServiceTest {

    // 14:00 UTC del 18/11 es el 18/11 10:00 en La Paz.
    private static final Instant AHORA = Instant.parse("2026-11-18T14:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 18);

    @Mock
    private PanelRepository repositorio;

    @Mock
    private UsuarioService usuarioService;

    private PanelService service;

    @BeforeEach
    void preparar() {
        service = new PanelService(repositorio, usuarioService, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void administrador_reunelosConteosYLaConvocatoriaAbierta() {
        UUID codigo = UUID.randomUUID();
        Instant cierre = Instant.parse("2026-12-01T04:00:00Z");
        when(repositorio.convocatoriasAbiertas()).thenReturn(1L);
        when(repositorio.postulacionesEnEstado("pendiente")).thenReturn(12L);
        when(repositorio.postulacionesEnEstado("calificada")).thenReturn(30L);
        when(repositorio.becasEnEstado("VIGENTE")).thenReturn(80L);
        when(repositorio.becasEnEstado("SUSPENDIDA")).thenReturn(3L);
        when(repositorio.asistenciasDelDia(HOY)).thenReturn(55L);
        when(repositorio.convocatoriaAbierta())
                .thenReturn(Optional.of(new ConvocatoriaAbiertaFila(codigo, "Beca 2026-II", cierre, 100, 42L)));

        PanelAdministradorResponse panel = service.administrador();

        assertThat(panel.convocatoriasAbiertas()).isEqualTo(1);
        assertThat(panel.postulacionesPendientes()).isEqualTo(12);
        assertThat(panel.postulacionesCalificadas()).isEqualTo(30);
        assertThat(panel.becasVigentes()).isEqualTo(80);
        assertThat(panel.becasSuspendidas()).isEqualTo(3);
        assertThat(panel.asistenciasHoy()).isEqualTo(55);
        assertThat(panel.convocatoriaAbierta().codigo()).isEqualTo(codigo);
        assertThat(panel.convocatoriaAbierta().postulaciones()).isEqualTo(42);
    }

    @Test
    void administrador_sinConvocatoriaAbierta_devuelveNull() {
        when(repositorio.convocatoriaAbierta()).thenReturn(Optional.empty());

        assertThat(service.administrador().convocatoriaAbierta()).isNull();
    }

    @Test
    void comedor_completaLosUltimosSieteDiasConCeros() {
        UUID actor = UUID.randomUUID();
        Usuario usuario = org.mockito.Mockito.mock(Usuario.class);
        when(usuario.getId()).thenReturn(7L);
        Instant ultimo = Instant.parse("2026-11-18T13:30:00Z");
        when(usuarioService.obtenerPorCodigo(actor)).thenReturn(usuario);
        when(repositorio.becasEnEstado("VIGENTE")).thenReturn(80L);
        when(repositorio.asistenciasDelDia(HOY)).thenReturn(40L);
        when(repositorio.asistenciasDelDiaRegistradasPor(HOY, 7L)).thenReturn(25L);
        when(repositorio.ultimoRegistroDelDia(HOY)).thenReturn(Optional.of(ultimo));
        when(repositorio.asistenciasPorFecha(HOY.minusDays(6), HOY)).thenReturn(
                List.of(new AsistenciaFecha(HOY, 40L), new AsistenciaFecha(HOY.minusDays(2), 10L)));

        PanelComedorResponse panel = service.comedor(actor);

        assertThat(panel.hoy()).isEqualTo(HOY);
        assertThat(panel.registradasPorMi()).isEqualTo(25);
        assertThat(panel.ultimoRegistro()).isEqualTo(ultimo);
        assertThat(panel.ultimosDias()).hasSize(7);
        assertThat(panel.ultimosDias().get(0).fecha()).isEqualTo(HOY.minusDays(6));
        assertThat(panel.ultimosDias().get(0).asistencias()).isZero();
        assertThat(panel.ultimosDias().get(4).asistencias()).isEqualTo(10);
        assertThat(panel.ultimosDias().get(6).asistencias()).isEqualTo(40);
    }

    private Usuario usuarioConId(UUID actor, long id) {
        Usuario usuario = org.mockito.Mockito.mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        when(usuarioService.obtenerPorCodigo(actor)).thenReturn(usuario);
        return usuario;
    }

    private ConvocatoriaAbiertaFila abierta(Integer cupos) {
        return new ConvocatoriaAbiertaFila(UUID.randomUUID(), "Beca", Instant.parse("2026-12-01T04:00:00Z"), cupos,
                99L);
    }

    @Test
    void postulante_conBorradorEnLaConvocatoriaAbierta_loIndica() {
        UUID actor = UUID.randomUUID();
        usuarioConId(actor, 9L);
        when(repositorio.convocatoriaAbierta()).thenReturn(Optional.of(abierta(10)));
        when(repositorio.estadoEnConvocatoriaAbierta(9L)).thenReturn(Optional.of("borrador"));

        var panel = service.postulante(actor);

        assertThat(panel.tieneBorrador()).isTrue();
        assertThat(panel.yaPostulo()).isFalse();
        assertThat(panel.convocatoriaAbierta().nombre()).isEqualTo("Beca");
    }

    @Test
    void postulante_conPostulacionEnviada_indicaQueYaPostulo() {
        UUID actor = UUID.randomUUID();
        usuarioConId(actor, 9L);
        when(repositorio.convocatoriaAbierta()).thenReturn(Optional.of(abierta(null)));
        when(repositorio.estadoEnConvocatoriaAbierta(9L)).thenReturn(Optional.of("pendiente"));

        var panel = service.postulante(actor);

        assertThat(panel.yaPostulo()).isTrue();
        assertThat(panel.tieneBorrador()).isFalse();
    }

    @Test
    void postulante_sinConvocatoriaAbierta_noTieneNadaQueMostrar() {
        UUID actor = UUID.randomUUID();
        usuarioConId(actor, 9L);
        when(repositorio.convocatoriaAbierta()).thenReturn(Optional.empty());

        var panel = service.postulante(actor);

        assertThat(panel.convocatoriaAbierta()).isNull();
        assertThat(panel.tieneBorrador()).isFalse();
        assertThat(panel.yaPostulo()).isFalse();
    }

    @Test
    void cadaPanelExigeElRolDeSuPantalla() throws NoSuchMethodException {
        Method admin = PanelService.class.getMethod("administrador");
        Method comedor = PanelService.class.getMethod("comedor", UUID.class);

        assertThat(admin.getAnnotation(PreAuthorize.class).value()).contains("ADMINISTRADOR_DUBSS");
        assertThat(comedor.getAnnotation(PreAuthorize.class).value()).contains("PERSONAL_COMEDOR");
        assertThat(PanelService.class.getMethod("postulante", UUID.class).getAnnotation(PreAuthorize.class).value())
                .contains("POSTULANTE");
    }
}
