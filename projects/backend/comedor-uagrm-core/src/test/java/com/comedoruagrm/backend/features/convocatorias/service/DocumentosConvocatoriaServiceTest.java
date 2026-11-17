package com.comedoruagrm.backend.features.convocatorias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.features.convocatorias.ConvocatoriaPruebas;
import com.comedoruagrm.backend.features.convocatorias.dto.DocumentoRequeridoRequest;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.ConvocatoriaDocumento;
import com.comedoruagrm.backend.features.convocatorias.model.TipoDocumento;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaDocumentoRepository;
import com.comedoruagrm.backend.features.convocatorias.repository.TipoDocumentoRepository;

class DocumentosConvocatoriaServiceTest {

    private final TipoDocumentoRepository tipoRepository = mock(TipoDocumentoRepository.class);
    private final ConvocatoriaDocumentoRepository documentoRepository = mock(ConvocatoriaDocumentoRepository.class);
    private final DocumentosConvocatoriaService service =
            new DocumentosConvocatoriaService(tipoRepository, documentoRepository);

    private final TipoDocumento carnet = ConvocatoriaPruebas.tipo(10L, "Carnet");
    private final TipoDocumento notas = ConvocatoriaPruebas.tipo(11L, "Certificado de notas");
    private final Convocatoria convocatoria = ConvocatoriaPruebas.convocatoria(
            1L, ConvocatoriaPruebas.gestion(1L, "2026-II"), EstadoConvocatoria.BORRADOR);

    private DocumentoRequeridoRequest req(TipoDocumento tipo, boolean obligatorio) {
        return new DocumentoRequeridoRequest(tipo.getCodigo(), obligatorio);
    }

    private ConvocatoriaDocumento vigente(TipoDocumento tipo, boolean obligatorio) {
        ConvocatoriaDocumento documento = mock(ConvocatoriaDocumento.class);
        when(documento.getTipoDocumento()).thenReturn(tipo);
        when(documento.isObligatorio()).thenReturn(obligatorio);
        return documento;
    }

    private void esperarError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ---- validar ----

    @Test
    void validar_conListaValida_devuelveCadaTipoConSuObligatoriedadEnOrden() {
        when(tipoRepository.findByCodigoIn(any())).thenReturn(List.of(notas, carnet));

        Map<TipoDocumento, Boolean> resultado = service.validar(List.of(req(carnet, true), req(notas, false)));

        assertThat(resultado).containsExactly(Map.entry(carnet, true), Map.entry(notas, false));
    }

    @Test
    void validar_conUnTipoRepetido_lanzaErrCv06() {
        esperarError(() -> service.validar(List.of(req(carnet, true), req(carnet, false))),
                ConvocatoriaErrorCodes.ERR_CV_06);
    }

    @Test
    void validar_sinNingunObligatorio_lanzaErrCv05() {
        esperarError(() -> service.validar(List.of(req(carnet, false), req(notas, false))),
                ConvocatoriaErrorCodes.ERR_CV_05);
    }

    @Test
    void validar_conUnTipoInexistente_lanzaErrCv03() {
        when(tipoRepository.findByCodigoIn(any())).thenReturn(List.of(carnet));

        esperarError(() -> service.validar(List.of(req(carnet, true),
                new DocumentoRequeridoRequest(UUID.randomUUID(), false))), ConvocatoriaErrorCodes.ERR_CV_03);
    }

    // ---- difiere ----

    @Test
    void difiere_conLosMismosTiposYObligatoriedad_esFalso() {
        ConvocatoriaDocumento enCarnet = vigente(carnet, true);
        when(documentoRepository.findByConvocatoriaId(1L)).thenReturn(List.of(enCarnet));

        assertThat(service.difiere(convocatoria, Map.of(carnet, true))).isFalse();
    }

    @Test
    void difiere_conOtraObligatoriedadOOtrosTipos_esVerdadero() {
        ConvocatoriaDocumento enCarnet = vigente(carnet, true);
        when(documentoRepository.findByConvocatoriaId(1L)).thenReturn(List.of(enCarnet));

        assertThat(service.difiere(convocatoria, Map.of(carnet, false))).isTrue();
        assertThat(service.difiere(convocatoria, Map.of(carnet, true, notas, false))).isTrue();
    }

    // ---- reemplazar ----

    @Test
    void reemplazar_quitaLosQueYaNoEstan_conBorradoLogico() {
        ConvocatoriaDocumento enCarnet = vigente(carnet, true);
        ConvocatoriaDocumento enNotas = vigente(notas, false);
        when(documentoRepository.findByConvocatoriaId(1L)).thenReturn(List.of(enCarnet, enNotas));

        boolean cambio = service.reemplazar(convocatoria, Map.of(carnet, true));

        assertThat(cambio).isTrue();
        verify(enNotas).setEliminadoEn(any());
        verify(documentoRepository, never()).reactivar(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void reemplazar_actualizaLaObligatoriedadDelQueSigue() {
        ConvocatoriaDocumento enCarnet = vigente(carnet, true);
        when(documentoRepository.findByConvocatoriaId(1L)).thenReturn(List.of(enCarnet));

        boolean cambio = service.reemplazar(convocatoria, Map.of(carnet, false));

        assertThat(cambio).isTrue();
        verify(enCarnet).setObligatorio(false);
    }

    @Test
    void reemplazar_unTipoQueSeHabiaQuitado_reactivaLaFilaEnVezDeInsertar() {
        when(documentoRepository.findByConvocatoriaId(1L)).thenReturn(List.of());
        when(documentoRepository.reactivar(1L, 10L, true)).thenReturn(1);

        boolean cambio = service.reemplazar(convocatoria, Map.of(carnet, true));

        assertThat(cambio).isTrue();
        verify(documentoRepository, never()).saveAndFlush(any(ConvocatoriaDocumento.class));
    }

    @Test
    void reemplazar_unTipoNuncaAgregado_loInserta() {
        when(documentoRepository.findByConvocatoriaId(1L)).thenReturn(List.of());
        when(documentoRepository.reactivar(1L, 10L, true)).thenReturn(0);

        service.reemplazar(convocatoria, Map.of(carnet, true));

        verify(documentoRepository).saveAndFlush(any(ConvocatoriaDocumento.class));
    }

    @Test
    void reemplazar_sinCambios_devuelveFalso() {
        ConvocatoriaDocumento enCarnet = vigente(carnet, true);
        when(documentoRepository.findByConvocatoriaId(1L)).thenReturn(List.of(enCarnet));

        assertThat(service.reemplazar(convocatoria, Map.of(carnet, true))).isFalse();
    }

    // ---- eliminarTodos ----

    @Test
    void eliminarTodos_marcaCadaDocumentoComoEliminado() {
        ConvocatoriaDocumento a = vigente(carnet, true);
        ConvocatoriaDocumento b = vigente(notas, false);
        when(documentoRepository.findByConvocatoriaId(1L)).thenReturn(List.of(a, b));

        service.eliminarTodos(convocatoria);

        verify(a).setEliminadoEn(any());
        verify(b).setEliminadoEn(any());
    }
}
