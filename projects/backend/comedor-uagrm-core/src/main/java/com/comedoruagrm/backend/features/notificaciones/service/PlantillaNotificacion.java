package com.comedoruagrm.backend.features.notificaciones.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.enums.TipoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.exception.PlantillaIncompletaException;

/** Rellena la plantilla de un evento con los datos de quien notifica (CU-EXTRA-01). */
@Component
public class PlantillaNotificacion {

    private static final Pattern MARCADOR = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9]*)\\}");

    /** El aviso ya armado: lo que se guarda en la bandeja y lo que viaja por push y correo. */
    public record NotificacionRenderizada(TipoNotificacion tipo, String titulo, String mensaje) {
    }

    public NotificacionRenderizada renderizar(EventoNotificacion evento, Map<String, String> datos) {
        return new NotificacionRenderizada(evento.tipo(),
                sustituir(evento, evento.titulo(), datos), sustituir(evento, evento.mensaje(), datos));
    }

    private String sustituir(EventoNotificacion evento, String plantilla, Map<String, String> datos) {
        Matcher marcador = MARCADOR.matcher(plantilla);
        StringBuilder resultado = new StringBuilder();
        while (marcador.find()) {
            String valor = datos.get(marcador.group(1));
            if (valor == null) {
                throw new PlantillaIncompletaException(evento.name(), marcador.group(1));
            }
            // quoteReplacement: un valor con "$" o "\" no se interpreta como referencia.
            marcador.appendReplacement(resultado, Matcher.quoteReplacement(valor));
        }
        marcador.appendTail(resultado);
        return resultado.toString();
    }
}
