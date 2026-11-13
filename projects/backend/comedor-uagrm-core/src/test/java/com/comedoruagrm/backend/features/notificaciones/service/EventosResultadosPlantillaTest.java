package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.exception.PlantillaIncompletaException;
import com.comedoruagrm.backend.features.notificaciones.service.PlantillaNotificacion.NotificacionRenderizada;

/** Plantillas de los avisos de la publicacion de resultados (CU22): tipo, marcadores y canales. */
class EventosResultadosPlantillaTest {

    private final PlantillaNotificacion plantilla = new PlantillaNotificacion();
    private final Map<String, String> datos = Map.of("nombres", "Ana", "convocatoria", "Beca Comedor 2026-II");

    @Test
    void adjudicada_esSuccess_reemplazaLosMarcadores_yEnviaPorLosTresCanales() {
        NotificacionRenderizada aviso = plantilla.renderizar(EventoNotificacion.BECA_ADJUDICADA, datos);

        assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.SUCCESS);
        assertThat(aviso.titulo()).isEqualTo("Beca Comedor: resultado de tu postulación");
        assertThat(aviso.mensaje()).isEqualTo("Ana, ¡felicidades! Tu postulación en Beca Comedor 2026-II fue "
                + "adjudicada: obtuviste la Beca Comedor.").doesNotContain("{");
        assertThat(EventoNotificacion.BECA_ADJUDICADA.enviaCorreo()).isTrue();
    }

    @Test
    void noAdjudicada_esInfo_reemplazaLosMarcadores_yEnviaPorLosTresCanales() {
        NotificacionRenderizada aviso = plantilla.renderizar(EventoNotificacion.BECA_NO_ADJUDICADA, datos);

        assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.INFO);
        assertThat(aviso.titulo()).isEqualTo("Beca Comedor: resultado de tu postulación");
        assertThat(aviso.mensaje()).isEqualTo("Ana, tu postulación en Beca Comedor 2026-II no fue adjudicada. "
                + "Gracias por participar.").doesNotContain("{");
        assertThat(EventoNotificacion.BECA_NO_ADJUDICADA.enviaCorreo()).isTrue();
    }

    @Test
    void sinUnMarcador_fallaEnVezDeEnviarUnTextoIncompleto() {
        assertThatThrownBy(() -> plantilla.renderizar(EventoNotificacion.BECA_ADJUDICADA, Map.of("nombres", "Ana")))
                .isInstanceOf(PlantillaIncompletaException.class);
        assertThatThrownBy(() -> plantilla.renderizar(EventoNotificacion.BECA_NO_ADJUDICADA,
                Map.of("convocatoria", "X"))).isInstanceOf(PlantillaIncompletaException.class);
    }

    @Test
    void titulos_noPasanDe150Caracteres() {
        assertThat(EventoNotificacion.BECA_ADJUDICADA.titulo().length()).isLessThanOrEqualTo(150);
        assertThat(EventoNotificacion.BECA_NO_ADJUDICADA.titulo().length()).isLessThanOrEqualTo(150);
    }
}
