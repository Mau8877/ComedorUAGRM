package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.PlantillaNotificacion.NotificacionRenderizada;

/** Plantillas de los avisos de la auditoria del expediente (CU07): tipo, marcadores y sin correo. */
class EventosAuditoriaPlantillaTest {

    private final PlantillaNotificacion plantilla = new PlantillaNotificacion();

    @Test
    void validada_esSuccess_usaSoloNombresYNoEnviaCorreo() {
        NotificacionRenderizada aviso = plantilla.renderizar(EventoNotificacion.AUDITORIA_VALIDADA,
                Map.of("nombres", "Ana"));

        assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.SUCCESS);
        assertThat(aviso.mensaje()).contains("Hola Ana").doesNotContain("{");
        assertThat(EventoNotificacion.AUDITORIA_VALIDADA.enviaCorreo()).isFalse();
    }

    @Test
    void observada_esWarning_yLlevaLasObservaciones() {
        NotificacionRenderizada aviso = plantilla.renderizar(EventoNotificacion.AUDITORIA_OBSERVADA,
                Map.of("nombres", "Ana", "observaciones", "Falta el certificado de notas"));

        assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.WARNING);
        assertThat(aviso.mensaje()).contains("Falta el certificado de notas");
        assertThat(EventoNotificacion.AUDITORIA_OBSERVADA.enviaCorreo()).isFalse();
    }

    @Test
    void noValida_esError_yLlevaElMotivo() {
        NotificacionRenderizada aviso = plantilla.renderizar(EventoNotificacion.AUDITORIA_NO_VALIDA,
                Map.of("nombres", "Ana", "observaciones", "La documentación no coincide"));

        assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.ERROR);
        assertThat(aviso.mensaje()).contains("La documentación no coincide");
        assertThat(EventoNotificacion.AUDITORIA_NO_VALIDA.enviaCorreo()).isFalse();
    }

    @Test
    void titulos_noPasanDe150Caracteres() {
        assertThat(EventoNotificacion.AUDITORIA_VALIDADA.titulo().length()).isLessThanOrEqualTo(150);
        assertThat(EventoNotificacion.AUDITORIA_OBSERVADA.titulo().length()).isLessThanOrEqualTo(150);
        assertThat(EventoNotificacion.AUDITORIA_NO_VALIDA.titulo().length()).isLessThanOrEqualTo(150);
    }
}
