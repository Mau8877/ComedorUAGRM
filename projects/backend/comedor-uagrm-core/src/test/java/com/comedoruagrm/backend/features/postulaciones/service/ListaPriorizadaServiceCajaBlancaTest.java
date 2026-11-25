package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.CarreraPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaService.PaginaListaPriorizada;
import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.ListaPriorizadaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.ListaPriorizadaRepository.DatosConvocatoria;

/**
 * Pruebas de caja blanca de CU-06 (capitulo 9, 9.3.2): un test por camino basico de
 * `ListaPriorizadaService.listarPriorizados` (C1 a C6).
 */
class ListaPriorizadaServiceCajaBlancaTest {

    private final ListaPriorizadaRepository repository = mock(ListaPriorizadaRepository.class);
    private final ListaPriorizadaService service = new ListaPriorizadaService(repository);
    private final UUID convocatoria = UUID.randomUUID();

    private PostulacionPriorizadaResponse fila(long posicion) {
        return new PostulacionPriorizadaResponse(UUID.randomUUID(), posicion, "Ana", "Rojas", "1234567",
                new CarreraPriorizadaResponse(UUID.randomUUID(), "Ingenieria de Sistemas"),
                new BigDecimal("95.0"), Instant.parse("2026-09-01T10:00:00Z"), "calificada");
    }

    private void existeLaConvocatoria() {
        when(repository.buscarConvocatoria(convocatoria))
                .thenReturn(Optional.of(new DatosConvocatoria(1L, "Beca 2026-II", "2026-II")));
    }

    private void fallaConCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(codigo));
        verify(repository, never()).contar(any(), any(), any());
    }

    @Test
    void listarPriorizados_C1_paginaMenorA1_lanzaErrSys01() {
        fallaConCodigo(() -> service.listarPriorizados(convocatoria, null, null, 0, 20, null), "ERR_SYS_01");
    }

    @Test
    void listarPriorizados_C2_tamanoMenorA1_lanzaErrSys01() {
        fallaConCodigo(() -> service.listarPriorizados(convocatoria, null, null, 1, 0, null), "ERR_SYS_01");
    }

    @Test
    void listarPriorizados_C3_sortConValor_lanzaErrPri02() {
        fallaConCodigo(() -> service.listarPriorizados(convocatoria, null, null, 1, 20, "puntaje"),
                PriorizacionErrorCodes.ERR_PRI_02);
    }

    @Test
    void listarPriorizados_C4_desplazamientoMayorOIgualAlTotal_devuelveLaPaginaVacia() {
        existeLaConvocatoria();
        when(repository.contar(convocatoria, null, null)).thenReturn(5L);

        PaginaListaPriorizada pagina = service.listarPriorizados(convocatoria, null, null, 3, 20, null);

        assertThat(pagina.datos()).isEmpty();
        assertThat(pagina.meta().totalItems()).isEqualTo(5);
        verify(repository, never()).buscar(any(), any(), any(), anyInt(), anyLong());
    }

    @Test
    void listarPriorizados_C5_hayDatos_devuelveLaPaginaSolicitada() {
        existeLaConvocatoria();
        when(repository.contar(convocatoria, null, null)).thenReturn(45L);
        when(repository.buscar(convocatoria, null, null, 20, 20L)).thenReturn(List.of(fila(21), fila(22)));

        PaginaListaPriorizada pagina = service.listarPriorizados(convocatoria, null, null, 2, 20, null);

        assertThat(pagina.datos()).extracting(PostulacionPriorizadaResponse::posicion).containsExactly(21L, 22L);
        assertThat(pagina.meta().totalPages()).isEqualTo(3);
    }

    @Test
    void listarPriorizados_C6_sortEnBlanco_noSeRechazaYDevuelveLaPagina() {
        existeLaConvocatoria();
        when(repository.contar(convocatoria, null, null)).thenReturn(45L);
        when(repository.buscar(convocatoria, null, null, 20, 0L)).thenReturn(List.of(fila(1)));

        PaginaListaPriorizada pagina = service.listarPriorizados(convocatoria, null, null, 1, 20, "   ");

        assertThat(pagina.datos()).hasSize(1);
        assertThat(pagina.meta().page()).isEqualTo(1);
    }
}
