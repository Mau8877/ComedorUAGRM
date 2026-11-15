package com.comedoruagrm.backend.features.convocatorias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.features.convocatorias.ConvocatoriaPruebas;
import com.comedoruagrm.backend.features.convocatorias.dto.GestionAcademicaRequest;
import com.comedoruagrm.backend.features.convocatorias.dto.GestionAcademicaResponse;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;
import com.comedoruagrm.backend.features.convocatorias.repository.GestionAcademicaRepository;
import com.comedoruagrm.backend.features.convocatorias.service.GestionAcademicaService.PaginaGestiones;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;

class GestionAcademicaServiceTest {

    private final GestionAcademicaRepository gestionRepository = mock(GestionAcademicaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final GestionAcademicaService service = new GestionAcademicaService(gestionRepository, usuarioRepository);

    private final UUID adminCodigo = UUID.randomUUID();
    private final Usuario admin = mock(Usuario.class);

    @BeforeEach
    void preparar() {
        when(admin.getId()).thenReturn(1L);
        when(admin.getCodigo()).thenReturn(adminCodigo);
        when(usuarioRepository.findByCodigo(adminCodigo)).thenReturn(Optional.of(admin));
        when(gestionRepository.saveAndFlush(any(GestionAcademica.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private GestionAcademicaRequest request(String etiqueta, LocalDate inicio, LocalDate fin) {
        return new GestionAcademicaRequest(etiqueta, inicio, fin);
    }

    private void esperarError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ---- listar ----

    @Test
    void listar_mapeaLaPaginaYSuMeta() {
        GestionAcademica gestion = ConvocatoriaPruebas.gestion(5L, "2026-II");
        when(gestionRepository.buscar(any(), any())).thenReturn(
                new PageImpl<>(List.of(gestion), PageRequest.of(0, 20), 41));

        PaginaGestiones pagina = service.listar(1, 20, null, null);

        assertThat(pagina.datos()).extracting(GestionAcademicaResponse::etiqueta).containsExactly("2026-II");
        assertThat(pagina.meta().totalItems()).isEqualTo(41);
        assertThat(pagina.meta().totalPages()).isEqualTo(3);
    }

    @Test
    void listar_sinSort_ordenaPorFechaDeInicioDescendente() {
        when(gestionRepository.buscar(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.listar(1, 20, "  2026 ", null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(gestionRepository).buscar(eq("%2026%"), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("fechaInicio").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listar_conSortNoSoportado_lanzaErrCv11() {
        esperarError(() -> service.listar(1, 20, null, "ci"), ConvocatoriaErrorCodes.ERR_CV_11);
    }

    @Test
    void listar_conPaginaCero_lanzaValidationException() {
        esperarError(() -> service.listar(0, 20, null, null), "ERR_SYS_01");
    }

    // ---- crear ----

    @Test
    void crear_conDatosValidos_guardaYAnotaElAlta() {
        ResultadoAuditado<GestionAcademicaResponse> resultado = service.crear(adminCodigo,
                request("  2026-II ", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20)));

        assertThat(resultado.datos().etiqueta()).isEqualTo("2026-II");
        assertThat(resultado.actorId()).isEqualTo(1L);
        assertThat(resultado.detalle()).containsEntry("etiqueta", "2026-II");
        assertThat(resultado.auditar()).isTrue();
    }

    @Test
    void crear_conEtiquetaRepetida_lanzaErrCv12SinGuardar() {
        when(gestionRepository.existsByEtiqueta("2026-II")).thenReturn(true);

        esperarError(() -> service.crear(adminCodigo,
                request("2026-II", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20))),
                ConvocatoriaErrorCodes.ERR_CV_12);
        verify(gestionRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_conFinIgualOAnteriorAlInicio_lanzaErrCv13() {
        LocalDate dia = LocalDate.of(2026, 7, 1);

        esperarError(() -> service.crear(adminCodigo, request("X", dia, dia)), ConvocatoriaErrorCodes.ERR_CV_13);
        esperarError(() -> service.crear(adminCodigo, request("X", dia, dia.minusDays(1))),
                ConvocatoriaErrorCodes.ERR_CV_13);
        verify(gestionRepository, never()).saveAndFlush(any());
    }

    // ---- editar ----

    @Test
    void editar_cambiaSoloLoQueDifiere() {
        GestionAcademica gestion = ConvocatoriaPruebas.gestion(5L, "2026-II");
        when(gestionRepository.findByCodigo(gestion.getCodigo())).thenReturn(Optional.of(gestion));

        ResultadoAuditado<GestionAcademicaResponse> resultado = service.editar(adminCodigo, gestion.getCodigo(),
                request("2026-II", gestion.getFechaInicio(), LocalDate.of(2026, 12, 30)));

        assertThat(gestion.getFechaFin()).isEqualTo(LocalDate.of(2026, 12, 30));
        assertThat(resultado.detalle()).containsEntry("camposModificados", List.of("fechaFin"));
        assertThat(resultado.auditar()).isTrue();
    }

    @Test
    void editar_sinCambios_noSeAnotaNiSeGuarda() {
        GestionAcademica gestion = ConvocatoriaPruebas.gestion(5L, "2026-II");
        when(gestionRepository.findByCodigo(gestion.getCodigo())).thenReturn(Optional.of(gestion));

        ResultadoAuditado<GestionAcademicaResponse> resultado = service.editar(adminCodigo, gestion.getCodigo(),
                request("2026-II", gestion.getFechaInicio(), gestion.getFechaFin()));

        assertThat(resultado.auditar()).isFalse();
        verify(gestionRepository, never()).saveAndFlush(any());
    }

    @Test
    void editar_conLaEtiquetaDeOtraGestion_lanzaErrCv12() {
        GestionAcademica gestion = ConvocatoriaPruebas.gestion(5L, "2026-II");
        when(gestionRepository.findByCodigo(gestion.getCodigo())).thenReturn(Optional.of(gestion));
        when(gestionRepository.existsByEtiquetaAndIdNot("2026-I", 5L)).thenReturn(true);

        esperarError(() -> service.editar(adminCodigo, gestion.getCodigo(),
                request("2026-I", gestion.getFechaInicio(), gestion.getFechaFin())),
                ConvocatoriaErrorCodes.ERR_CV_12);
    }

    @Test
    void editar_conSuPropiaEtiqueta_noEsConflicto() {
        GestionAcademica gestion = ConvocatoriaPruebas.gestion(5L, "2026-II");
        when(gestionRepository.findByCodigo(gestion.getCodigo())).thenReturn(Optional.of(gestion));

        service.editar(adminCodigo, gestion.getCodigo(),
                request("2026-II", gestion.getFechaInicio(), LocalDate.of(2026, 12, 30)));

        verify(gestionRepository, never()).existsByEtiquetaAndIdNot(any(), any());
    }

    @Test
    void editar_inexistente_lanzaErrCv02() {
        esperarError(() -> service.editar(adminCodigo, UUID.randomUUID(),
                request("X", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20))),
                ConvocatoriaErrorCodes.ERR_CV_02);
    }

    @Test
    void editar_conFinNoPosteriorAlInicio_lanzaErrCv13() {
        GestionAcademica gestion = ConvocatoriaPruebas.gestion(5L, "2026-II");
        when(gestionRepository.findByCodigo(gestion.getCodigo())).thenReturn(Optional.of(gestion));

        esperarError(() -> service.editar(adminCodigo, gestion.getCodigo(),
                request("2026-II", gestion.getFechaInicio(), gestion.getFechaInicio())),
                ConvocatoriaErrorCodes.ERR_CV_13);
    }
}
