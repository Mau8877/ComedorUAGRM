package com.comedoruagrm.backend.features.convocatorias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.features.convocatorias.ConvocatoriaPruebas;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResponse;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.mapper.ConvocatoriaMapper;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaRepository;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;

class ConvocatoriaEstadoServiceTest {

    private final ConvocatoriaRepository convocatoriaRepository = mock(ConvocatoriaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ConvocatoriaMapper mapper = mock(ConvocatoriaMapper.class);
    private final ConvocatoriaEstadoService service =
            new ConvocatoriaEstadoService(convocatoriaRepository, usuarioRepository, mapper);

    private final UUID adminCodigo = UUID.randomUUID();
    private final Usuario admin = mock(Usuario.class);
    private final GestionAcademica gestion = ConvocatoriaPruebas.gestion(1L, "2026-II");

    @BeforeEach
    void preparar() {
        when(admin.getId()).thenReturn(7L);
        when(admin.getCodigo()).thenReturn(adminCodigo);
        when(usuarioRepository.findByCodigo(adminCodigo)).thenReturn(Optional.of(admin));
        when(convocatoriaRepository.saveAndFlush(any(Convocatoria.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.detalle(any())).thenAnswer(inv -> ConvocatoriaPruebas.detalle(inv.getArgument(0)));
    }

    private Convocatoria existente(long id, EstadoConvocatoria estado) {
        Convocatoria convocatoria = ConvocatoriaPruebas.convocatoria(id, gestion, estado);
        when(convocatoriaRepository.findByCodigo(convocatoria.getCodigo())).thenReturn(Optional.of(convocatoria));
        when(convocatoriaRepository.findById(id)).thenReturn(Optional.of(convocatoria));
        return convocatoria;
    }

    private void esperarError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ================= abrir =================

    @Test
    void abrir_unaBorradorSinOtraAbierta_laPasaAAbierta() {
        Convocatoria convocatoria = existente(5L, EstadoConvocatoria.BORRADOR);

        ResultadoAuditado<ConvocatoriaResponse> resultado = service.abrir(adminCodigo, convocatoria.getCodigo());

        assertThat(convocatoria.getEstado()).isEqualTo(EstadoConvocatoria.ABIERTA);
        assertThat(resultado.detalle()).containsEntry("estadoAnterior", "BORRADOR")
                .containsEntry("estadoNuevo", "ABIERTA");
        assertThat(resultado.actorId()).isEqualTo(7L);
        assertThat(resultado.auditar()).isTrue();
    }

    @Test
    void abrir_conOtraAbiertaEnLaMismaGestion_lanzaErrCv07() {
        Convocatoria convocatoria = existente(5L, EstadoConvocatoria.BORRADOR);
        when(convocatoriaRepository.existsByGestionIdAndEstadoAndIdNot(1L, EstadoConvocatoria.ABIERTA, 5L))
                .thenReturn(true);

        esperarError(() -> service.abrir(adminCodigo, convocatoria.getCodigo()), ConvocatoriaErrorCodes.ERR_CV_07);
        assertThat(convocatoria.getEstado()).isEqualTo(EstadoConvocatoria.BORRADOR);
        verify(convocatoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void abrir_conOtraAbiertaPeroEnOtraGestion_sePuede() {
        Convocatoria convocatoria = existente(5L, EstadoConvocatoria.BORRADOR);
        // El repositorio se consulta por la gestion de esta convocatoria: si esa no tiene otra abierta, abre.
        when(convocatoriaRepository.existsByGestionIdAndEstadoAndIdNot(2L, EstadoConvocatoria.ABIERTA, 9L))
                .thenReturn(true);

        service.abrir(adminCodigo, convocatoria.getCodigo());

        assertThat(convocatoria.getEstado()).isEqualTo(EstadoConvocatoria.ABIERTA);
    }

    @Test
    void abrir_siElIndiceUnicoRechazaLaApertura_lanzaErrCv07() {
        Convocatoria convocatoria = existente(5L, EstadoConvocatoria.BORRADOR);
        when(convocatoriaRepository.saveAndFlush(any(Convocatoria.class))).thenThrow(
                new DataIntegrityViolationException("x",
                        new RuntimeException("duplicate key value violates unique constraint "
                                + "\"uq_convocatoria_abierta_por_gestion\"")));

        esperarError(() -> service.abrir(adminCodigo, convocatoria.getCodigo()), ConvocatoriaErrorCodes.ERR_CV_07);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoConvocatoria.class, names = {"ABIERTA", "CERRADA", "PUBLICADA"})
    void abrir_siNoEstaEnBorrador_lanzaErrCv08(EstadoConvocatoria estado) {
        Convocatoria convocatoria = existente(5L, estado);

        esperarError(() -> service.abrir(adminCodigo, convocatoria.getCodigo()), ConvocatoriaErrorCodes.ERR_CV_08);
        assertThat(convocatoria.getEstado()).isEqualTo(estado);
    }

    @Test
    void abrir_inexistente_lanzaErrCv01() {
        esperarError(() -> service.abrir(adminCodigo, UUID.randomUUID()), ConvocatoriaErrorCodes.ERR_CV_01);
    }

    // ================= cerrar =================

    @Test
    void cerrar_unaAbierta_laPasaACerrada() {
        Convocatoria convocatoria = existente(5L, EstadoConvocatoria.ABIERTA);

        ResultadoAuditado<ConvocatoriaResponse> resultado = service.cerrar(adminCodigo, convocatoria.getCodigo());

        assertThat(convocatoria.getEstado()).isEqualTo(EstadoConvocatoria.CERRADA);
        assertThat(resultado.detalle()).containsEntry("estadoAnterior", "ABIERTA")
                .containsEntry("estadoNuevo", "CERRADA");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoConvocatoria.class, names = {"BORRADOR", "CERRADA", "PUBLICADA"})
    void cerrar_siNoEstaAbierta_lanzaErrCv08(EstadoConvocatoria estado) {
        Convocatoria convocatoria = existente(5L, estado);

        esperarError(() -> service.cerrar(adminCodigo, convocatoria.getCodigo()), ConvocatoriaErrorCodes.ERR_CV_08);
        assertThat(convocatoria.getEstado()).isEqualTo(estado);
    }

    @Test
    void cerrar_inexistente_lanzaErrCv01() {
        esperarError(() -> service.cerrar(adminCodigo, UUID.randomUUID()), ConvocatoriaErrorCodes.ERR_CV_01);
    }

    // ================= cerrarVencida (cierre automatico) =================

    @Test
    void cerrarVencida_unaAbiertaConElCierreVencido_laCierraConActorNulo() {
        Convocatoria convocatoria = existente(5L, EstadoConvocatoria.ABIERTA);
        // La convocatoria de prueba cierra el 31 de agosto de 2026; el reloj real ya paso esa fecha o no segun
        // cuando se corra, asi que se fuerza el cierre al pasado.
        convocatoria.setFechaCierre(Instant.now().minusSeconds(60));

        ResultadoAuditado<Void> resultado = service.cerrarVencida(5L);

        assertThat(convocatoria.getEstado()).isEqualTo(EstadoConvocatoria.CERRADA);
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.actorId()).isNull();
        assertThat(resultado.actorCodigo()).isNull();
        assertThat(resultado.entidadId()).isEqualTo(5L);
        assertThat(resultado.detalle()).containsEntry("motivo", "fecha_cierre");
    }

    @Test
    void cerrarVencida_siTodaviaNoVence_noHaceNada() {
        Convocatoria convocatoria = existente(5L, EstadoConvocatoria.ABIERTA);
        convocatoria.setFechaCierre(Instant.now().plusSeconds(3600));

        ResultadoAuditado<Void> resultado = service.cerrarVencida(5L);

        assertThat(convocatoria.getEstado()).isEqualTo(EstadoConvocatoria.ABIERTA);
        assertThat(resultado.auditar()).isFalse();
        verify(convocatoriaRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoConvocatoria.class, names = {"BORRADOR", "CERRADA", "PUBLICADA"})
    void cerrarVencida_siNoEstaAbierta_noHaceNadaNiAnotaNada(EstadoConvocatoria estado) {
        Convocatoria convocatoria = existente(5L, estado);
        convocatoria.setFechaCierre(Instant.now().minusSeconds(60));

        ResultadoAuditado<Void> resultado = service.cerrarVencida(5L);

        assertThat(convocatoria.getEstado()).isEqualTo(estado);
        assertThat(resultado.auditar()).isFalse();
    }

    @Test
    void cerrarVencida_siLaConvocatoriaYaNoExiste_noHaceNada() {
        assertThat(service.cerrarVencida(99L).auditar()).isFalse();
    }
}
