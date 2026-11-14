package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.postulaciones.dto.MiPostulacionDetalleResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PasoLineaTiempoResponse;
import com.comedoruagrm.backend.features.postulaciones.enums.SituacionPaso;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.CabeceraDetalle;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.DatosFinalista;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.FilaHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.MisPostulacionesConsultaRepository.FilaListado;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** El detalle «Mis postulaciones» (CU04): una rama por estado visible, con los colaboradores simulados. */
@ExtendWith(MockitoExtension.class)
class MisPostulacionesDetalleServiceTest {

    private static final long USUARIO_ID = 9L;
    private static final long POSTULACION_ID = 70L;
    private static final Instant ENVIO = Instant.parse("2026-11-01T15:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-03T15:00:00Z");
    private static final Instant T3 = Instant.parse("2026-11-05T15:00:00Z");

    @Mock
    private MisPostulacionesConsultaRepository consulta;

    @Mock
    private UsuarioService usuarioService;

    private MisPostulacionesService service;
    private final UUID usuarioCodigo = UUID.randomUUID();
    private final UUID postulacion = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new MisPostulacionesService(consulta, usuarioService);
        Usuario postulante = new Usuario(null, "1111111", "p@prueba.test", "hash", "Pia", "Perez");
        ReflectionTestUtils.setField(postulante, "id", USUARIO_ID);
        ReflectionTestUtils.setField(postulante, "codigo", usuarioCodigo);
        lenient().when(usuarioService.obtenerPorCodigo(usuarioCodigo)).thenReturn(postulante);
        lenient().when(consulta.historial(POSTULACION_ID)).thenReturn(List.of());
        lenient().when(consulta.finalista(POSTULACION_ID)).thenReturn(Optional.empty());
        lenient().when(consulta.decisionFinal(POSTULACION_ID)).thenReturn(Optional.empty());
    }

    /** Deja la postulacion en ese estado real con esas marcas de publicacion y decision final. */
    private void postulacion(String estado, boolean publicada, boolean decisionFinal) {
        FilaListado fila = new FilaListado(postulacion, UUID.randomUUID(), "Beca 2026-II", UUID.randomUUID(),
                "2026-II", ENVIO, estado, (short) 2, publicada, decisionFinal);
        when(consulta.cabecera(USUARIO_ID, postulacion)).thenReturn(Optional.of(new CabeceraDetalle(POSTULACION_ID, fila)));
    }

    private void historial(FilaHistorial... filas) {
        when(consulta.historial(POSTULACION_ID)).thenReturn(List.of(filas));
    }

    private static FilaHistorial fh(String nuevo, Instant fecha, String motivo) {
        return new FilaHistorial(nuevo, fecha, motivo);
    }

    private static List<String> estados(MiPostulacionDetalleResponse d, SituacionPaso situacion) {
        return d.lineaDeTiempo().stream().filter(p -> p.situacion() == situacion)
                .map(PasoLineaTiempoResponse::estado).toList();
    }

    // ---- cabecera y estado visible ----

    @Test
    void detalle_pendiente_muestraElResumenYLosPasosQueFaltan() {
        postulacion("pendiente", false, false);
        historial(fh("pendiente", ENVIO, null));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.codigo()).isEqualTo(postulacion);
        assertThat(d.convocatoria().nombre()).isEqualTo("Beca 2026-II");
        assertThat(d.gestionAcademica().nombre()).isEqualTo("2026-II");
        assertThat(d.fechaEnvio()).isEqualTo(ENVIO);
        assertThat(d.estado()).isEqualTo("pendiente");
        assertThat(d.numeroEnvios()).isEqualTo((short) 2);
        assertThat(estados(d, SituacionPaso.COMPLETADO)).containsExactly("pendiente");
        assertThat(estados(d, SituacionPaso.PENDIENTE)).containsExactly("calificada", "resultado");
        assertThat(d.observaciones()).isNull();
        assertThat(d.motivo()).isNull();
        assertThat(d.finalista()).isNull();
        assertThat(d.resultado()).isNull();
        assertThat(d.puedeCorregir()).isFalse();
    }

    @Test
    void detalle_laLineaDeTiempoSaleDelHistorialEnOrden_conSuFecha_ySinInventarPasos() {
        postulacion("calificada", false, false);
        historial(fh("pendiente", ENVIO, null), fh("calificada", T2, null));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.lineaDeTiempo()).containsExactly(
                new PasoLineaTiempoResponse("pendiente", ENVIO, SituacionPaso.COMPLETADO),
                new PasoLineaTiempoResponse("calificada", T2, SituacionPaso.COMPLETADO),
                new PasoLineaTiempoResponse("resultado", null, SituacionPaso.PENDIENTE));
    }

    @Test
    void detalle_sinFilasDeHistorial_noFabricaPasosPasados() {
        postulacion("pendiente", false, false);

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(estados(d, SituacionPaso.COMPLETADO)).isEmpty();
    }

    // ---- devuelta ----

    @Test
    void detalle_devuelta_muestraLasObservacionesDeLaUltimaDevolucion_yPuedeCorregir() {
        postulacion("devuelta", false, false);
        historial(fh("pendiente", ENVIO, null), fh("calificada", T2, null), fh("devuelta", T2.plusSeconds(60), "Falta el CI"),
                fh("pendiente", T3, null), fh("calificada", T3.plusSeconds(60), null),
                fh("devuelta", T3.plusSeconds(120), "Falta el certificado"));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.estado()).isEqualTo("devuelta");
        assertThat(d.observaciones().texto()).isEqualTo("Falta el certificado");
        assertThat(d.observaciones().fecha()).isEqualTo(T3.plusSeconds(120));
        assertThat(d.puedeCorregir()).isTrue();
        assertThat(d.motivo()).isNull();
        assertThat(estados(d, SituacionPaso.PENDIENTE)).containsExactly("pendiente", "calificada", "resultado");
    }

    // ---- archivada y rechazo (CU20) ----

    @Test
    void detalle_archivada_muestraElMotivoDelArchivo_sinPasosPendientes() {
        postulacion("archivada", false, false);
        historial(fh("pendiente", ENVIO, null), fh("calificada", T2, null), fh("archivada", T3, "No cumple el requisito"));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.estado()).isEqualTo("archivada");
        assertThat(d.motivo()).isEqualTo("No cumple el requisito");
        assertThat(estados(d, SituacionPaso.PENDIENTE)).isEmpty();
        assertThat(d.puedeCorregir()).isFalse();
    }

    @Test
    void detalle_rechazoDelCu20_seVeNoAdjudicadaDeInmediato_conSuMotivo_yFilaVisible() {
        postulacion("no_adjudicada", false, false);
        historial(fh("pendiente", ENVIO, null), fh("calificada", T2, null),
                fh("no_adjudicada", T3, "Documentos falsificados"));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.estado()).isEqualTo("no_adjudicada");
        assertThat(d.motivo()).isEqualTo("Documentos falsificados");
        assertThat(estados(d, SituacionPaso.COMPLETADO)).containsExactly("pendiente", "calificada", "no_adjudicada");
        assertThat(d.resultado()).isNull();
        verify(consulta, never()).decisionFinal(anyLong());
    }

    // ---- decision final (CU05) ----

    @Test
    void detalle_decisionFinalSinPublicar_seVeCalificada_ocultaLaFila_ySinResultado() {
        postulacion("adjudicada", false, true);
        historial(fh("pendiente", ENVIO, null), fh("calificada", T2, null), fh("adjudicada", T3, null));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.estado()).isEqualTo("calificada");
        assertThat(estados(d, SituacionPaso.COMPLETADO)).containsExactly("pendiente", "calificada");
        assertThat(estados(d, SituacionPaso.PENDIENTE)).containsExactly("resultado");
        assertThat(d.resultado()).isNull();
        assertThat(d.motivo()).isNull();
        verify(consulta, never()).decisionFinal(anyLong());
    }

    @Test
    void detalle_noAdjudicadaPorDecisionFinalSinPublicar_ocultaLaFilaYElMotivo() {
        postulacion("no_adjudicada", false, true);
        historial(fh("pendiente", ENVIO, null), fh("calificada", T2, null),
                fh("no_adjudicada", T3, "Motivo reservado a la DUBSS"));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.estado()).isEqualTo("calificada");
        assertThat(estados(d, SituacionPaso.COMPLETADO)).doesNotContain("no_adjudicada");
        assertThat(d.toString()).doesNotContain("Motivo reservado");
    }

    @Test
    void detalle_adjudicadaPublicada_muestraElResultadoYLaFila() {
        postulacion("adjudicada", true, true);
        historial(fh("pendiente", ENVIO, null), fh("calificada", T2, null), fh("adjudicada", T3, null));
        when(consulta.decisionFinal(POSTULACION_ID)).thenReturn(Optional.of("ADJUDICADA"));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.estado()).isEqualTo("adjudicada");
        assertThat(d.resultado()).isEqualTo("ADJUDICADA");
        assertThat(estados(d, SituacionPaso.COMPLETADO)).containsExactly("pendiente", "calificada", "adjudicada");
        assertThat(estados(d, SituacionPaso.PENDIENTE)).isEmpty();
    }

    @Test
    void detalle_noAdjudicadaPorDecisionFinalPublicada_muestraElResultadoSinElMotivo() {
        postulacion("no_adjudicada", true, true);
        historial(fh("pendiente", ENVIO, null), fh("calificada", T2, null),
                fh("no_adjudicada", T3, "Motivo reservado a la DUBSS"));
        when(consulta.decisionFinal(POSTULACION_ID)).thenReturn(Optional.of("NO_ADJUDICADA"));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.estado()).isEqualTo("no_adjudicada");
        assertThat(d.resultado()).isEqualTo("NO_ADJUDICADA");
        assertThat(d.motivo()).isNull();
        assertThat(d.toString()).doesNotContain("Motivo reservado");
    }

    // ---- finalista ----

    @Test
    void detalle_finalistaConAvisoYSinDocumentacionRecibida_muestraPlazoYDocumentos() {
        postulacion("calificada", false, false);
        when(consulta.finalista(POSTULACION_ID))
                .thenReturn(Optional.of(new DatosFinalista(LocalDate.parse("2026-11-20"), false)));
        when(consulta.mensajeDelAvisoDeFinalista(POSTULACION_ID)).thenReturn(Optional.of(
                "Hola Pia, quedaste como finalista de la Beca Comedor. Debes entregar tu documentación física "
                        + "hasta el 20/11/2026. Lugar y horario: DUBSS, 8 a 12. Documentos a presentar: CI, "
                        + "certificado de notas."));

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.finalista().plazoEntrega()).isEqualTo(LocalDate.parse("2026-11-20"));
        assertThat(d.finalista().documentacionRecibida()).isFalse();
        assertThat(d.finalista().documentosFisicos()).isEqualTo("CI, certificado de notas");
    }

    @Test
    void detalle_finalistaConDocumentacionRecibida_ySinAviso_dejaLosDocumentosEnNulo() {
        postulacion("calificada", false, false);
        when(consulta.finalista(POSTULACION_ID)).thenReturn(Optional.of(new DatosFinalista(null, true)));
        when(consulta.mensajeDelAvisoDeFinalista(POSTULACION_ID)).thenReturn(Optional.empty());

        MiPostulacionDetalleResponse d = service.detalle(usuarioCodigo, postulacion);

        assertThat(d.finalista().documentacionRecibida()).isTrue();
        assertThat(d.finalista().plazoEntrega()).isNull();
        assertThat(d.finalista().documentosFisicos()).isNull();
    }

    @Test
    void documentosDelAviso_quitaElPuntoFinalDeLaPlantilla_yTolerarMensajesSinLaMarca() {
        assertThat(MisPostulacionesService.documentosDelAviso("... Documentos a presentar: Solo el CI.")).isEqualTo("Solo el CI");
        assertThat(MisPostulacionesService.documentosDelAviso("Mensaje sin la marca")).isNull();
        assertThat(MisPostulacionesService.documentosDelAviso("Documentos a presentar: .")).isNull();
    }

    @Test
    void detalle_sinSerFinalista_noPideElAviso() {
        postulacion("pendiente", false, false);

        service.detalle(usuarioCodigo, postulacion);

        verify(consulta, never()).mensajeDelAvisoDeFinalista(anyLong());
    }

    // ---- 404, permisos y privacidad ----

    @Test
    void detalle_deUnaPostulacionAjenaInexistenteOBorrador_dan404ErrPos01ConElMismoMensaje() {
        when(consulta.cabecera(USUARIO_ID, postulacion)).thenReturn(Optional.empty());
        UUID otra = UUID.randomUUID();
        when(consulta.cabecera(USUARIO_ID, otra)).thenReturn(Optional.empty());

        Throwable ajena = org.assertj.core.api.Assertions.catchThrowable(() -> service.detalle(usuarioCodigo, postulacion));
        Throwable inexistente = org.assertj.core.api.Assertions.catchThrowable(() -> service.detalle(usuarioCodigo, otra));

        assertThat(ajena).isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_POS_01"));
        assertThat(inexistente).hasMessage(ajena.getMessage());
        assertThatThrownBy(() -> service.detalle(usuarioCodigo, otra)).isInstanceOf(NotFoundException.class);
        verify(consulta, never()).historial(anyLong());
    }

    @Test
    void detalle_exigeElRolPostulante_esDeSoloLectura_yElDtoNoLlevaAutorNiDatosPersonales() throws NoSuchMethodException {
        var metodo = MisPostulacionesService.class.getMethod("detalle", UUID.class, UUID.class);

        assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('POSTULANTE')");
        assertThat(metodo.getAnnotation(Transactional.class).readOnly()).isTrue();
        List<String> campos = Arrays.stream(PasoLineaTiempoResponse.class.getRecordComponents())
                .map(RecordComponent::getName).toList();
        assertThat(campos).containsExactly("estado", "fecha", "situacion");
        List<String> detalle = Arrays.stream(MiPostulacionDetalleResponse.class.getRecordComponents())
                .map(RecordComponent::getName).toList();
        assertThat(detalle).doesNotContain("id", "ci", "correo", "usuario", "usuarioId", "autor", "hechoPor");
    }
}
