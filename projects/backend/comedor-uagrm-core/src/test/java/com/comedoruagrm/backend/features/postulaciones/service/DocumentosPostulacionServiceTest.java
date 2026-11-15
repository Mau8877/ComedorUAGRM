package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.postulaciones.dto.AccesoDocumentoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentoAdjuntoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DocumentosPostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.exception.DocumentoPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.DocumentoAdjunto;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentoAdjuntoRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Adjunto;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Cabecera;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Requerido;

class DocumentosPostulacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-07-16T15:00:00Z");
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', 0, 0};

    private final DocumentosPostulacionConsultaRepository consulta = mock(DocumentosPostulacionConsultaRepository.class);
    private final DocumentoAdjuntoRepository documentos = mock(DocumentoAdjuntoRepository.class);
    private final AlmacenamientoService almacenamiento = mock(AlmacenamientoService.class);
    private final ReglasArchivoDocumento reglas = new ReglasArchivoDocumento(1024,
            Set.of("application/pdf", "image/jpeg", "image/png"), 2);
    private final DocumentosPostulacionService service = new DocumentosPostulacionService(consulta, documentos,
            almacenamiento, reglas, Clock.fixed(AHORA, ZoneOffset.UTC));

    private final UUID usuario = UUID.randomUUID();
    private final UUID postulacion = UUID.randomUUID();
    private final UUID tipoCodigo = UUID.randomUUID();
    private final UUID documentoCodigo = UUID.randomUUID();
    private final ArchivoSubido archivo = new ArchivoSubido("carnet.pdf", "application/pdf", PDF);

    private Cabecera cabecera(String estado, String estadoConvocatoria, Instant cierre) {
        return new Cabecera(3L, postulacion, estado, 5L, estadoConvocatoria, cierre);
    }

    private void dadaLaPostulacion(String estado) {
        when(consulta.cabecera(postulacion, usuario)).thenReturn(
                Optional.of(cabecera(estado, "ABIERTA", AHORA.plusSeconds(86400))));
    }

    private static String codigoDe(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    private DocumentoAdjunto existente() {
        DocumentoAdjunto d = new DocumentoAdjunto(3L, 9L, "viejo.pdf", "application/pdf", "ruta/vieja.pdf", 10L,
                AHORA.minusSeconds(60));
        ReflectionTestUtils.setField(d, "codigo", documentoCodigo);
        when(documentos.findByCodigoAndPostulacionId(documentoCodigo, 3L)).thenReturn(Optional.of(d));
        return d;
    }

    // ---- listar ----

    @Test
    void listar_muestraLosRequeridosConSuEstadoYSusArchivos() {
        dadaLaPostulacion("borrador");
        when(consulta.requeridos(5L)).thenReturn(List.of(
                new Requerido(9L, tipoCodigo, "Carnet", "Ambas caras", true),
                new Requerido(10L, UUID.randomUUID(), "Notas", null, false)));
        when(consulta.adjuntos(3L)).thenReturn(List.of(
                new Adjunto(documentoCodigo, 9L, "ci.pdf", "application/pdf", 100L, AHORA)));

        DocumentosPostulacionResponse r = service.listar(usuario, postulacion);

        assertThat(r.puedeEditar()).isTrue();
        assertThat(r.requeridos()).hasSize(2);
        assertThat(r.requeridos().get(0).adjuntado()).isTrue();
        assertThat(r.requeridos().get(0).obligatorio()).isTrue();
        assertThat(r.requeridos().get(0).archivos()).extracting(DocumentoAdjuntoResponse::codigo)
                .containsExactly(documentoCodigo);
        assertThat(r.requeridos().get(1).adjuntado()).isFalse();
        assertThat(r.requeridos().get(1).archivos()).isEmpty();
        assertThat(r.reglas().maximoArchivosPorTipo()).isEqualTo(2);
        assertThat(r.reglas().tamanoMaximoBytes()).isEqualTo(1024);
        assertThat(r.reglas().formatosPermitidos()).containsExactly("application/pdf", "image/jpeg", "image/png");
    }

    @Test
    void listar_conPostulacionEnviada_sePuedeLeerPeroNoEditar() {
        dadaLaPostulacion("pendiente");
        when(consulta.requeridos(5L)).thenReturn(List.of());
        when(consulta.adjuntos(3L)).thenReturn(List.of());

        assertThat(service.listar(usuario, postulacion).puedeEditar()).isFalse();
    }

    @Test
    void listar_conPostulacionDevuelta_sePuedeEditar() {
        dadaLaPostulacion("devuelta");
        when(consulta.requeridos(5L)).thenReturn(List.of());
        when(consulta.adjuntos(3L)).thenReturn(List.of());

        assertThat(service.listar(usuario, postulacion).puedeEditar()).isTrue();
    }

    @Test
    void listar_conConvocatoriaCerrada_noSePuedeEditar() {
        when(consulta.cabecera(postulacion, usuario)).thenReturn(
                Optional.of(cabecera("borrador", "CERRADA", AHORA.plusSeconds(86400))));
        when(consulta.requeridos(5L)).thenReturn(List.of());
        when(consulta.adjuntos(3L)).thenReturn(List.of());

        assertThat(service.listar(usuario, postulacion).puedeEditar()).isFalse();
    }

    @Test
    void listar_conPostulacionInexistenteOAjena_lanzaErrDoc04() {
        when(consulta.cabecera(postulacion, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(usuario, postulacion)).isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_04));
    }

    // ---- adjuntar ----

    @Test
    void adjuntar_guardaElArchivoYRegistraLaFila() {
        dadaLaPostulacion("borrador");
        when(consulta.tipoRequerido(5L, tipoCodigo)).thenReturn(Optional.of(9L));
        when(documentos.countByPostulacionIdAndTipoDocumentoId(3L, 9L)).thenReturn(0L);
        when(documentos.saveAndFlush(any(DocumentoAdjunto.class))).thenAnswer(i -> i.getArgument(0));

        DocumentoAdjuntoResponse r = service.adjuntar(usuario, postulacion, tipoCodigo, archivo);

        ArgumentCaptor<String> ruta = ArgumentCaptor.forClass(String.class);
        verify(almacenamiento).subir(ruta.capture(), eq(PDF), eq("application/pdf"));
        assertThat(ruta.getValue()).startsWith("postulaciones/" + postulacion + "/documentos/").endsWith(".pdf")
                .doesNotContain("carnet");
        ArgumentCaptor<DocumentoAdjunto> fila = ArgumentCaptor.forClass(DocumentoAdjunto.class);
        verify(documentos).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getPostulacionId()).isEqualTo(3L);
        assertThat(fila.getValue().getTipoDocumentoId()).isEqualTo(9L);
        assertThat(fila.getValue().getStoragePath()).isEqualTo(ruta.getValue());
        assertThat(fila.getValue().getTamanoBytes()).isEqualTo(PDF.length);
        assertThat(fila.getValue().getSubidoEn()).isEqualTo(AHORA);
        assertThat(r.nombreArchivo()).isEqualTo("carnet.pdf");
        verify(consulta).bloquearPostulacion(3L);
    }

    @Test
    void adjuntar_conPostulacionDevuelta_loPermite() {
        dadaLaPostulacion("devuelta");
        when(consulta.tipoRequerido(5L, tipoCodigo)).thenReturn(Optional.of(9L));
        when(documentos.saveAndFlush(any(DocumentoAdjunto.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.adjuntar(usuario, postulacion, tipoCodigo, archivo)).isNotNull();
    }

    @Test
    void adjuntar_conPostulacionEnviada_lanzaErrDoc06SinTocarElAlmacenamiento() {
        for (String estado : List.of("pendiente", "calificada", "archivada", "adjudicada", "no_adjudicada")) {
            dadaLaPostulacion(estado);

            assertThatThrownBy(() -> service.adjuntar(usuario, postulacion, tipoCodigo, archivo))
                    .isInstanceOf(ConflictException.class)
                    .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_06));
        }
        verify(almacenamiento, never()).subir(anyString(), any(), anyString());
    }

    @Test
    void adjuntar_conConvocatoriaCerrada_lanzaErrDoc07() {
        when(consulta.cabecera(postulacion, usuario)).thenReturn(
                Optional.of(cabecera("borrador", "CERRADA", AHORA.plusSeconds(86400))));

        assertThatThrownBy(() -> service.adjuntar(usuario, postulacion, tipoCodigo, archivo))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_07));
    }

    @Test
    void adjuntar_conConvocatoriaAbiertaPeroYaVencida_lanzaErrDoc07() {
        when(consulta.cabecera(postulacion, usuario)).thenReturn(
                Optional.of(cabecera("borrador", "ABIERTA", AHORA.minusSeconds(1))));

        assertThatThrownBy(() -> service.adjuntar(usuario, postulacion, tipoCodigo, archivo))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_07));
    }

    @Test
    void adjuntar_conTipoQueNoEstaEnLaLista_lanzaErrDoc05() {
        dadaLaPostulacion("borrador");
        when(consulta.tipoRequerido(5L, tipoCodigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjuntar(usuario, postulacion, tipoCodigo, archivo))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_05));
    }

    @Test
    void adjuntar_conArchivoInvalido_laRechazaAntesDeGuardar() {
        dadaLaPostulacion("borrador");
        when(consulta.tipoRequerido(5L, tipoCodigo)).thenReturn(Optional.of(9L));

        assertThatThrownBy(() -> service.adjuntar(usuario, postulacion, tipoCodigo,
                new ArchivoSubido("x.pdf", "application/pdf", "no es un pdf".getBytes())))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_03));
        verify(almacenamiento, never()).subir(anyString(), any(), anyString());
        verify(documentos, never()).saveAndFlush(any());
    }

    @Test
    void adjuntar_conElMaximoDeArchivosDelTipo_lanzaErrDoc08() {
        dadaLaPostulacion("borrador");
        when(consulta.tipoRequerido(5L, tipoCodigo)).thenReturn(Optional.of(9L));
        when(documentos.countByPostulacionIdAndTipoDocumentoId(3L, 9L)).thenReturn(2L);

        assertThatThrownBy(() -> service.adjuntar(usuario, postulacion, tipoCodigo, archivo))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_08));
        verify(almacenamiento, never()).subir(anyString(), any(), anyString());
    }

    @Test
    void adjuntar_siFallaElAlmacenamiento_noRegistraLaFila() {
        dadaLaPostulacion("borrador");
        when(consulta.tipoRequerido(5L, tipoCodigo)).thenReturn(Optional.of(9L));
        doThrow(new AlmacenamientoException("caido", new RuntimeException())).when(almacenamiento)
                .subir(anyString(), any(), anyString());

        assertThatThrownBy(() -> service.adjuntar(usuario, postulacion, tipoCodigo, archivo))
                .isInstanceOf(AlmacenamientoException.class);
        verify(documentos, never()).saveAndFlush(any());
    }

    @Test
    void adjuntar_siFallaElRegistro_retiraElArchivoRecienSubido() {
        dadaLaPostulacion("borrador");
        when(consulta.tipoRequerido(5L, tipoCodigo)).thenReturn(Optional.of(9L));
        when(documentos.saveAndFlush(any(DocumentoAdjunto.class))).thenThrow(new IllegalStateException("bd"));

        assertThatThrownBy(() -> service.adjuntar(usuario, postulacion, tipoCodigo, archivo))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> subida = ArgumentCaptor.forClass(String.class);
        verify(almacenamiento).subir(subida.capture(), any(), anyString());
        verify(almacenamiento).eliminar(subida.getValue());
    }

    // ---- reemplazar ----

    @Test
    void reemplazar_cambiaElArchivoEnLaMismaFilaYRetiraElAnterior() {
        dadaLaPostulacion("devuelta");
        DocumentoAdjunto d = existente();
        when(documentos.saveAndFlush(d)).thenReturn(d);

        DocumentoAdjuntoResponse r = service.reemplazar(usuario, postulacion, documentoCodigo, archivo);

        ArgumentCaptor<String> nueva = ArgumentCaptor.forClass(String.class);
        verify(almacenamiento).subir(nueva.capture(), eq(PDF), eq("application/pdf"));
        assertThat(d.getStoragePath()).isEqualTo(nueva.getValue()).isNotEqualTo("ruta/vieja.pdf");
        assertThat(d.getNombreArchivo()).isEqualTo("carnet.pdf");
        assertThat(d.getSubidoEn()).isEqualTo(AHORA);
        assertThat(r.codigo()).isEqualTo(documentoCodigo);
        verify(almacenamiento).eliminar("ruta/vieja.pdf");
    }

    @Test
    void reemplazar_conPostulacionEnviada_lanzaErrDoc06() {
        dadaLaPostulacion("pendiente");

        assertThatThrownBy(() -> service.reemplazar(usuario, postulacion, documentoCodigo, archivo))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_06));
    }

    @Test
    void reemplazar_conDocumentoInexistenteOAjeno_lanzaErrDoc09() {
        dadaLaPostulacion("borrador");
        when(documentos.findByCodigoAndPostulacionId(documentoCodigo, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reemplazar(usuario, postulacion, documentoCodigo, archivo))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_09));
        verify(almacenamiento, never()).subir(anyString(), any(), anyString());
    }

    @Test
    void reemplazar_siFallaElRegistro_retiraElNuevoYConservaElAnterior() {
        dadaLaPostulacion("borrador");
        DocumentoAdjunto d = existente();
        when(documentos.saveAndFlush(d)).thenThrow(new IllegalStateException("bd"));

        assertThatThrownBy(() -> service.reemplazar(usuario, postulacion, documentoCodigo, archivo))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> nueva = ArgumentCaptor.forClass(String.class);
        verify(almacenamiento).subir(nueva.capture(), any(), anyString());
        verify(almacenamiento).eliminar(nueva.getValue());
        verify(almacenamiento, never()).eliminar("ruta/vieja.pdf");
    }

    // ---- eliminar ----

    @Test
    void eliminar_marcaElBorradoLogicoYRetiraElArchivo() {
        dadaLaPostulacion("borrador");
        DocumentoAdjunto d = existente();

        service.eliminar(usuario, postulacion, documentoCodigo);

        assertThat(d.getEliminadoEn()).isEqualTo(AHORA);
        verify(documentos).saveAndFlush(d);
        verify(almacenamiento).eliminar("ruta/vieja.pdf");
    }

    @Test
    void eliminar_conPostulacionEnviada_lanzaErrDoc06YNoToca() {
        dadaLaPostulacion("calificada");

        assertThatThrownBy(() -> service.eliminar(usuario, postulacion, documentoCodigo))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_06));
        verify(almacenamiento, never()).eliminar(anyString());
        verify(documentos, never()).saveAndFlush(any());
    }

    @Test
    void eliminar_siElAlmacenamientoFalla_laFilaYaQuedoEliminadaYNoSePropaga() {
        dadaLaPostulacion("borrador");
        existente();
        doThrow(new AlmacenamientoException("caido", new RuntimeException())).when(almacenamiento)
                .eliminar(anyString());

        service.eliminar(usuario, postulacion, documentoCodigo);

        verify(documentos).saveAndFlush(any(DocumentoAdjunto.class));
    }

    // ---- acceso ----

    @Test
    void acceso_daUnaUrlTemporalYNoLaRuta() {
        dadaLaPostulacion("pendiente");
        existente();
        when(almacenamiento.urlTemporal("ruta/vieja.pdf", AlmacenamientoService.VIGENCIA_ACCESO))
                .thenReturn("https://archivos.test/firmada");

        AccesoDocumentoResponse r = service.acceso(usuario, postulacion, documentoCodigo);

        assertThat(r.url()).isEqualTo("https://archivos.test/firmada");
        assertThat(r.expiraEn()).isEqualTo(AHORA.plus(AlmacenamientoService.VIGENCIA_ACCESO));
    }

    @Test
    void acceso_conDocumentoAjeno_lanzaErrDoc09() {
        dadaLaPostulacion("pendiente");
        when(documentos.findByCodigoAndPostulacionId(any(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceso(usuario, postulacion, documentoCodigo))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(DocumentoPostulacionErrorCodes.ERR_DOC_09));
        verify(almacenamiento, never()).urlTemporal(anyString(), any());
    }

    // ---- nombre ----

    @Test
    void nombreSeguro_quitaCarpetasYCaracteresDeControl() {
        ReglasArchivoDocumento.Formato pdf = new ReglasArchivoDocumento.Formato("application/pdf", "pdf");

        assertThat(DocumentosPostulacionService.nombreSeguro("C:\\fotos\\..\\carnet\u0000.pdf", pdf))
                .isEqualTo("carnet.pdf");
        assertThat(DocumentosPostulacionService.nombreSeguro("../../etc/passwd", pdf)).isEqualTo("passwd");
        assertThat(DocumentosPostulacionService.nombreSeguro("  ", pdf)).isEqualTo("documento.pdf");
        assertThat(DocumentosPostulacionService.nombreSeguro(null, pdf)).isEqualTo("documento.pdf");
        assertThat(DocumentosPostulacionService.nombreSeguro("a".repeat(300) + ".pdf", pdf)).hasSize(255);
    }
}
