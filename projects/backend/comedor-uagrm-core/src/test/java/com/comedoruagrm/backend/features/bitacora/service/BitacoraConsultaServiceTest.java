package com.comedoruagrm.backend.features.bitacora.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.bitacora.exception.BitacoraErrorCodes;
import com.comedoruagrm.backend.features.bitacora.model.Bitacora;
import com.comedoruagrm.backend.features.bitacora.repository.BitacoraRepository;
import com.comedoruagrm.backend.features.bitacora.service.BitacoraConsultaService.PaginaBitacora;

@ExtendWith(MockitoExtension.class)
class BitacoraConsultaServiceTest {

    @Mock
    private BitacoraRepository repository;

    @InjectMocks
    private BitacoraConsultaService service;

    @Test
    void listar_sinFiltros_ordenaPorFechaDescendenteYArmaElMeta() {
        Page<Bitacora> vacia = new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0);
        when(repository.buscar(any(), any(), any(), any(), any(Pageable.class))).thenReturn(vacia);

        PaginaBitacora pagina = service.listar(1, 20, null, null, null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).buscar(eq(null), eq(null), eq(null), eq(null), captor.capture());
        Sort.Order orden = captor.getValue().getSort().getOrderFor("creadoEn");
        assertThat(orden).isNotNull();
        assertThat(orden.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pagina.datos()).isEmpty();
        assertThat(pagina.meta().page()).isEqualTo(1);
        assertThat(pagina.meta().pageSize()).isEqualTo(20);
    }

    @Test
    void listar_conFechas_incluyeElDiaCompletoEnHoraDeBolivia() {
        Page<Bitacora> vacia = new PageImpl<>(List.of());
        when(repository.buscar(any(), any(), any(), any(), any(Pageable.class))).thenReturn(vacia);

        service.listar(1, 20, "  Perez ", " ADJUDICACION_BECA ", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                "accion");

        ArgumentCaptor<Instant> desde = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> hasta = ArgumentCaptor.forClass(Instant.class);
        verify(repository).buscar(eq("%perez%"), eq("ADJUDICACION_BECA"), desde.capture(), hasta.capture(),
                any(Pageable.class));
        // 00:00 en La Paz (UTC-4) del 1/9 y 00:00 del 1/10 (limite exclusivo).
        assertThat(desde.getValue()).isEqualTo(Instant.parse("2026-09-01T04:00:00Z"));
        assertThat(hasta.getValue()).isEqualTo(Instant.parse("2026-10-01T04:00:00Z"));
    }

    @Test
    void listar_conSortNoSoportado_lanzaErrBit01() {
        assertThatThrownBy(() -> service.listar(1, 20, null, null, null, null, "ip"))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode").isEqualTo(BitacoraErrorCodes.ERR_BIT_01);
        verifyNoInteractions(repository);
    }

    @Test
    void listar_conDesdePosteriorAHasta_lanzaErrBit02() {
        assertThatThrownBy(() -> service.listar(1, 20, null, null, LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 9, 1), null))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode").isEqualTo(BitacoraErrorCodes.ERR_BIT_02);
        verifyNoInteractions(repository);
    }

    @Test
    void listar_conPaginaCero_lanzaValidacionGenerica() {
        assertThatThrownBy(() -> service.listar(0, 20, null, null, null, null, null))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode").isEqualTo("ERR_SYS_01");
    }
}
