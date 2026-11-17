package com.comedoruagrm.backend.features.convocatorias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.features.convocatorias.ConvocatoriaPruebas;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.model.TipoDocumento;
import com.comedoruagrm.backend.features.convocatorias.repository.TipoDocumentoRepository;
import com.comedoruagrm.backend.features.convocatorias.service.TipoDocumentoService.PaginaTiposDocumento;

class TipoDocumentoServiceTest {

    private final TipoDocumentoRepository repository = mock(TipoDocumentoRepository.class);
    private final TipoDocumentoService service = new TipoDocumentoService(repository);

    @Test
    void listar_mapeaLaPaginaYSuMeta() {
        TipoDocumento carnet = ConvocatoriaPruebas.tipo(1L, "Carnet");
        when(repository.buscar(any(), any())).thenReturn(new PageImpl<>(
                List.of(carnet), PageRequest.of(0, 20), 41));

        PaginaTiposDocumento pagina = service.listar(1, 20, null, null);

        assertThat(pagina.datos()).extracting(t -> t.nombre()).containsExactly("Carnet");
        assertThat(pagina.meta().totalItems()).isEqualTo(41);
    }

    @Test
    void listar_sinSort_ordenaPorNombreAscendente() {
        when(repository.buscar(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.listar(1, 20, " Cert ", null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).buscar(eq("%cert%"), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("nombre").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void listar_conSortNoSoportado_lanzaErrCv11() {
        assertThatThrownBy(() -> service.listar(1, 20, null, "descripcion")).isInstanceOfSatisfying(
                BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ConvocatoriaErrorCodes.ERR_CV_11));
    }
}
