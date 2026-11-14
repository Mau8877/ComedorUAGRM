package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.PlantillaNotificacion.NotificacionRenderizada;

/** Plantillas de los avisos del cierre de una postulacion (CU20): tipo, marcadores, motivo y correo. */
class EventosCierrePostulacionPlantillaTest {

    private final PlantillaNotificacion plantilla = new PlantillaNotificacion();
    private final Map<String, String> datos = Map.of("nombres", "Ana", "motivo", "No entregó su documentación");

    @Test
    void archivada_esInfo_llevaNombresYMotivo_yEnviaCorreo() {
        NotificacionRenderizada aviso = plantilla.renderizar(EventoNotificacion.POSTULACION_ARCHIVADA, datos);

        assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.INFO);
        assertThat(aviso.titulo()).isEqualTo("Tu postulación fue archivada - Beca Comedor");
        assertThat(aviso.mensaje()).isEqualTo("Hola Ana, la DUBSS archivó tu postulación. "
                + "Motivo: No entregó su documentación").doesNotContain("{");
        assertThat(EventoNotificacion.POSTULACION_ARCHIVADA.enviaCorreo()).isTrue();
    }

    @Test
    void rechazada_esError_llevaNombresYMotivo_yEnviaCorreo() {
        NotificacionRenderizada aviso = plantilla.renderizar(EventoNotificacion.POSTULACION_RECHAZADA, datos);

        assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.ERROR);
        assertThat(aviso.titulo()).isEqualTo("Tu postulación no fue adjudicada - Beca Comedor");
        assertThat(aviso.mensaje()).isEqualTo("Hola Ana, la DUBSS no pudo adjudicarte la beca con esta "
                + "postulación. Motivo: No entregó su documentación").doesNotContain("{");
        assertThat(EventoNotificacion.POSTULACION_RECHAZADA.enviaCorreo()).isTrue();
    }

    @Test
    void titulos_noPasanDe150Caracteres() {
        assertThat(EventoNotificacion.POSTULACION_ARCHIVADA.titulo().length()).isLessThanOrEqualTo(150);
        assertThat(EventoNotificacion.POSTULACION_RECHAZADA.titulo().length()).isLessThanOrEqualTo(150);
    }
}
