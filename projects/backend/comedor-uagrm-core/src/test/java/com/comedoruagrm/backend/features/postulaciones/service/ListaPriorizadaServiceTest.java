package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.CarreraPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ResumenListaPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.repository.ListaPriorizadaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.ListaPriorizadaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaService.PaginaListaPriorizada;

class ListaPriorizadaServiceTest {

    private final ListaPriorizadaRepository repository = mock(ListaPriorizadaRepository.class);
    private final ListaPriorizadaService service = new ListaPriorizadaService(repository);

    private final UUID convocatoria = UUID.randomUUID();

    private PostulacionPriorizadaResponse fila(long posicion) {
        return new PostulacionPriorizadaResponse(UUID.randomUUID(), posicion, "Ana", "Rojas", "1234567",
                new CarreraPriorizadaResponse(UUID.randomUUID(), "Ingeniería de Sistemas"),
                new BigDecimal("95.0"), Instant.parse("2026-09-01T10:00:00Z"), "calificada");
    }

    private void existeLaConvocatoria() {
        when(repository.buscarConvocatoria(convocatoria))
                .thenReturn(Optional.of(new DatosConvocatoria(1L, "Beca 2026-II", "2026-II")));
    }

    @Test
    void listar_devuelveLaPaginaConSuMetaYLaPosicionQueManda_elRepositorio() {
        existeLaConvocatoria();
        when(repository.contar(convocatoria, null, null)).thenReturn(45L);
        when(repository.buscar(convocatoria, null, null, 20, 20L)).thenReturn(List.of(fila(21), fila(22)));

        PaginaListaPriorizada pagina = service.listarPriorizados(convocatoria, null, null, 2, 20, null);

        assertThat(pagina.datos()).extracting(PostulacionPriorizadaResponse::posicion).containsExactly(21L, 22L);
        assertThat(pagina.meta().page()).isEqualTo(2);
        assertThat(pagina.meta().pageSize()).isEqualTo(20);
        assertThat(pagina.meta().totalItems()).isEqualTo(45);
        assertThat(pagina.meta().totalPages()).isEqualTo(3);
    }

    @Test
    void listar_conUnaPaginaMasAllaDelFinal_devuelveVacioSinConsultarLasFilas() {
        existeLaConvocatoria();
        when(repository.contar(convocatoria, null, null)).thenReturn(5L);

        PaginaListaPriorizada pagina = service.listarPriorizados(convocatoria, null, null, 3, 20, null);

        assertThat(pagina.datos()).isEmpty();
        verify(repository, never()).buscar(any(), any(), any(), anyInt(), anyLong());
    }

    @Test
    void listar_listaVacia_devuelveMetaConCeroPaginas() {
        existeLaConvocatoria();
        when(repository.contar(convocatoria, null, null)).thenReturn(0L);

        PaginaListaPriorizada pagina = service.listarPriorizados(convocatoria, null, null, 1, 20, null);

        assertThat(pagina.datos()).isEmpty();
        assertThat(pagina.meta().totalItems()).isZero();
        assertThat(pagina.meta().totalPages()).isZero();
    }

    @Test
    void listar_conBusqueda_laPasaEnMinusculasConComodinesYEscapandoLosEspeciales() {
        existeLaConvocatoria();
        UUID carrera = UUID.randomUUID();
        when(repository.contar(eq(convocatoria), eq(carrera), any())).thenReturn(1L);

        service.listarPriorizados(convocatoria, "  Ana_% ", carrera, 1, 20, null);

        verify(repository).contar(convocatoria, carrera, "%ana\\_\\%%");
        verify(repository).buscar(convocatoria, carrera, "%ana\\_\\%%", 20, 0L);
    }

    @Test
    void listar_conUnSort_da400ErrPri02() {
        assertThatThrownBy(() -> service.listarPriorizados(convocatoria, null, null, 1, 20, "-puntaje"))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode").isEqualTo("ERR_PRI_02");
    }

    @Test
    void listar_conPaginaOTamanoInvalidos_da400ErrSys01() {
        assertThatThrownBy(() -> service.listarPriorizados(convocatoria, null, null, 0, 20, null))
                .isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo("ERR_SYS_01");
        assertThatThrownBy(() -> service.listarPriorizados(convocatoria, null, null, 1, 0, null))
                .isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo("ERR_SYS_01");
    }

    @Test
    void listar_conUnaConvocatoriaInexistente_da404ErrPri01() {
        when(repository.buscarConvocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPriorizados(convocatoria, null, null, 1, 20, null))
                .isInstanceOf(NotFoundException.class).extracting("errorCode").isEqualTo("ERR_PRI_01");
    }

    @Test
    void resumir_cuentaCalificadasYPendientes() {
        existeLaConvocatoria();
        when(repository.contar(convocatoria, null, null)).thenReturn(140L);
        when(repository.contarPendientes(convocatoria)).thenReturn(30L);

        ResumenListaPriorizadaResponse resumen = service.resumir(convocatoria);

        assertThat(resumen.totalCalificadas()).isEqualTo(140);
        assertThat(resumen.totalPendientes()).isEqualTo(30);
    }

    @Test
    void resumir_conUnaConvocatoriaInexistente_da404ErrPri01() {
        when(repository.buscarConvocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resumir(convocatoria))
                .isInstanceOf(NotFoundException.class).extracting("errorCode").isEqualTo("ERR_PRI_01");
    }

    @Test
    void listarConvocatorias_delegaEnElRepositorio() {
        when(repository.listarConvocatorias()).thenReturn(List.of());

        assertThat(service.listarConvocatorias()).isEmpty();
    }

    @Test
    void patronDeBusqueda_sinTextoDevuelveNull() {
        assertThat(ListaPriorizadaService.patronDeBusqueda(null)).isNull();
        assertThat(ListaPriorizadaService.patronDeBusqueda("   ")).isNull();
    }
}
