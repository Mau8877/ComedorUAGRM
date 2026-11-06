package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.exception.PlantillaIncompletaException;
import com.comedoruagrm.backend.features.notificaciones.service.PlantillaNotificacion.NotificacionRenderizada;

class PlantillaNotificacionTest {

    private final PlantillaNotificacion plantilla = new PlantillaNotificacion();

    @Test
    void renderizar_sustituyeLosMarcadoresConLosDatos() {
        NotificacionRenderizada aviso = plantilla.renderizar(EventoNotificacion.PRUEBA, Map.of("nombres", "Ana"));

        assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.INFO);
        assertThat(aviso.titulo()).isEqualTo("Notificación de prueba");
        assertThat(aviso.mensaje()).contains("Hola Ana,").doesNotContain("{nombres}");
    }

    @Test
    void renderizar_conUnDatoQueFalta_lanzaPlantillaIncompletaNombrandoLaClaveYNoLosValores() {
        assertThatThrownBy(() -> plantilla.renderizar(EventoNotificacion.PRUEBA, Map.of("otro", "secreto")))
                .isInstanceOf(PlantillaIncompletaException.class)
                .hasMessageContaining("nombres")
                .hasMessageNotContaining("secreto");
    }

    @Test
    void renderizar_conValoresConCaracteresEspeciales_noLosInterpretaComoReferencias() {
        NotificacionRenderizada aviso = plantilla.renderizar(EventoNotificacion.PRUEBA,
                Map.of("nombres", "Ana $1 \\ {nombres}"));

        assertThat(aviso.mensaje()).contains("Hola Ana $1 \\ {nombres},");
    }

    @ParameterizedTest
    @EnumSource(EventoNotificacion.class)
    void todoEvento_tieneTituloQueCabeEnLaColumnaYMensajeNoVacio(EventoNotificacion evento) {
        assertThat(evento.titulo()).isNotBlank().hasSizeLessThanOrEqualTo(150);
        assertThat(evento.mensaje()).isNotBlank();
        assertThat(evento.tipo()).isNotNull();
    }
}
