package com.comedoruagrm.backend.features.preseleccion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.comedoruagrm.backend.features.preseleccion.dto.PrecalificacionResponse;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository.CabeceraPrecalificacion;
import com.comedoruagrm.backend.features.preseleccion.repository.PrecalificacionConsultaRepository.CambioEstado;

/** La consulta de la precalificacion con IA de una postulacion (CU03). */
class PrecalificacionDetalleServiceTest {

    private static final String DETALLE = """
            {"variables": {
               "ingresos_familiares": {"nivel": 1.0, "ponderacion": 28.0, "puntos": 19.6},
               "dependientes": {"nivel": 0.5, "ponderacion": 12.0, "puntos": 4.2},
               "discapacidad": {"nivel": null, "ponderacion": 10.0, "puntos": 0.0}},
             "puntaje_sobre_100": 59.0, "puntaje_maximo": 70.0, "advertencias": ["sin dato: discapacidad"]}
            """;

    private final PrecalificacionConsultaRepository consulta = mock(PrecalificacionConsultaRepository.class);
    private final PrecalificacionDetalleService service = new PrecalificacionDetalleService(consulta);
    private final UUID codigo = UUID.randomUUID();

    private CabeceraPrecalificacion cabecera(String estado, BigDecimal puntaje, String detalle, int intentos,
            Instant revision) {
        return new CabeceraPrecalificacion(7L, codigo, "Ana", "Rojas", "9802041", "Convocatoria 2026-II", estado,
                intentos, revision, puntaje, puntaje == null ? null : "v1.0",
                puntaje == null ? null : Instant.parse("2026-11-01T10:00:00Z"), detalle);
    }

    @Test
    void obtener_deUnaCalificada_traeElPuntajeLaPosicionYElAporteDeCadaVariable() {
        when(consulta.cabecera(codigo)).thenReturn(
                Optional.of(cabecera("calificada", new BigDecimal("23.80"), DETALLE, 0, null)));
        when(consulta.posicion(7L)).thenReturn(Optional.of(5L));
        when(consulta.historial(7L)).thenReturn(List.of(
                new CambioEstado("pendiente", null, Instant.parse("2026-11-01T09:59:00Z")),
                new CambioEstado("calificada", "Precalificación automática", Instant.parse("2026-11-01T10:00:00Z"))));

        PrecalificacionResponse r = service.obtener(codigo);

        assertThat(r.puntaje()).isEqualByComparingTo("23.80");
        assertThat(r.puntajeMaximo()).isEqualByComparingTo("70");
        assertThat(r.modelo()).isEqualTo("v1.0");
        assertThat(r.posicion()).isEqualTo(5L);
        assertThat(r.variables()).hasSize(3);
        // máximo = ponderación / suma de las ponderaciones con dato (28 + 12) * 70
        assertThat(r.variables().get(0).maximo()).isEqualByComparingTo("49.00");
        assertThat(r.variables().get(1).maximo()).isEqualByComparingTo("21.00");
        // sin dato: no tiene máximo (no entra al puntaje)
        assertThat(r.variables().get(2).nivel()).isNull();
        assertThat(r.variables().get(2).maximo()).isNull();
        assertThat(r.advertencias()).containsExactly("sin dato: discapacidad");
        assertThat(r.marcadaParaRevision()).isFalse();
        assertThat(r.traza()).extracting("paso").containsExactly("Postulación enviada",
                "Precalificación automática", "Ubicación en la lista priorizada");
        assertThat(r.traza().get(2).detalle()).isEqualTo("Posición 5");
    }

    @Test
    void obtener_deUnaPendienteSinPuntaje_vieneSinPuntajeNiPosicionNiVariables() {
        when(consulta.cabecera(codigo)).thenReturn(Optional.of(cabecera("pendiente", null, null, 0, null)));
        when(consulta.historial(7L)).thenReturn(List.of());

        PrecalificacionResponse r = service.obtener(codigo);

        assertThat(r.puntaje()).isNull();
        assertThat(r.puntajeMaximo()).isNull();
        assertThat(r.posicion()).isNull();
        assertThat(r.variables()).isEmpty();
        verify(consulta, never()).posicion(7L);
    }

    @Test
    void obtener_deUnaMarcadaParaRevision_loDiceYLoPoneEnLaTraza() {
        Instant desde = Instant.parse("2026-11-02T08:00:00Z");
        when(consulta.cabecera(codigo)).thenReturn(Optional.of(cabecera("pendiente", null, null, 3, desde)));
        when(consulta.historial(7L)).thenReturn(List.of());

        PrecalificacionResponse r = service.obtener(codigo);

        assertThat(r.marcadaParaRevision()).isTrue();
        assertThat(r.revisionDesde()).isEqualTo(desde);
        assertThat(r.intentos()).isEqualTo(3);
        assertThat(r.traza()).extracting("paso").containsExactly("Marcada para revisión");
        assertThat(r.traza().get(0).fecha()).isEqualTo(desde);
    }

    @Test
    void obtener_conUnDetalleQueNoSeLee_noTumbaLaConsultaYVieneSinDesglose() {
        when(consulta.cabecera(codigo)).thenReturn(
                Optional.of(cabecera("calificada", new BigDecimal("40.00"), "esto no es json", 0, null)));
        when(consulta.posicion(7L)).thenReturn(Optional.of(1L));
        when(consulta.historial(7L)).thenReturn(List.of());

        PrecalificacionResponse r = service.obtener(codigo);

        assertThat(r.puntaje()).isEqualByComparingTo("40.00");
        assertThat(r.variables()).isEmpty();
        assertThat(r.puntajeMaximo()).isEqualByComparingTo("70");
    }

    @Test
    void obtener_deUnaPostulacionQueNoExiste_lanzaNotFound() {
        when(consulta.cabecera(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(codigo)).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo("ERR_MOD_08");
    }
}
