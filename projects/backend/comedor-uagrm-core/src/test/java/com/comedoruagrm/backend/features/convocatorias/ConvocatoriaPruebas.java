package com.comedoruagrm.backend.features.convocatorias;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResponse;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;
import com.comedoruagrm.backend.features.convocatorias.model.TipoDocumento;
import com.comedoruagrm.backend.features.convocatorias.service.FechasConvocatoria;

/**
 * Objetos de prueba del CU13. Los Models no tienen setter de `id` ni de `codigo` (los genera la base), asi que
 * los tests unitarios los fijan por reflexion.
 */
public final class ConvocatoriaPruebas {

    private ConvocatoriaPruebas() {
    }

    public static GestionAcademica gestion(long id, String etiqueta) {
        GestionAcademica gestion = new GestionAcademica(etiqueta, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20));
        ReflectionTestUtils.setField(gestion, "id", id);
        ReflectionTestUtils.setField(gestion, "codigo", UUID.randomUUID());
        return gestion;
    }

    public static TipoDocumento tipo(long id, String nombre) {
        TipoDocumento tipo = org.mockito.Mockito.mock(TipoDocumento.class);
        org.mockito.Mockito.when(tipo.getId()).thenReturn(id);
        org.mockito.Mockito.when(tipo.getCodigo()).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.when(tipo.getNombre()).thenReturn(nombre);
        return tipo;
    }

    /** Una convocatoria del 1 al 31 de agosto de 2026, en el estado pedido. */
    public static Convocatoria convocatoria(long id, GestionAcademica gestion, EstadoConvocatoria estado) {
        Convocatoria convocatoria = new Convocatoria(gestion, 1L, "Beca Comedor 2026-II",
                FechasConvocatoria.aperturaDesde(LocalDate.of(2026, 8, 1)),
                FechasConvocatoria.cierreHasta(LocalDate.of(2026, 8, 31)), 100, new BigDecimal("40.00"), 120);
        convocatoria.setEstado(estado);
        ReflectionTestUtils.setField(convocatoria, "id", id);
        ReflectionTestUtils.setField(convocatoria, "codigo", UUID.randomUUID());
        return convocatoria;
    }

    public static ConvocatoriaResponse detalle(Convocatoria convocatoria) {
        return ConvocatoriaResponse.de(convocatoria, List.of(), false);
    }
}
