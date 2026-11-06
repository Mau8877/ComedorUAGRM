package com.comedoruagrm.backend.features.notificaciones.enums;

/**
 * Catalogo de eventos que notifican, cada uno con su plantilla (CU-EXTRA-01).
 *
 * * El titulo y el mensaje usan marcadores `{clave}` que se rellenan con los datos que pasa
 * * quien notifica (ver `PlantillaNotificacion`). El titulo es tambien el asunto del correo y
 * * no pasa de 150 caracteres (limite de la columna).
 * * Para agregar un evento: una constante nueva aca, con los datos que su plantilla exige. Los
 * * eventos reales los agrega el CU que los necesita, no este.
 * ! Nada sensible en la plantilla: el texto sale por push y por correo.
 */
public enum EventoNotificacion {

    /** Aviso de prueba del Administrador DUBSS para comprobar que el push y el correo funcionan. */
    PRUEBA(TipoNotificacion.INFO,
            "Notificación de prueba",
            "Hola {nombres}, esta es una prueba del servicio de notificaciones de ComedorU.");

    private final TipoNotificacion tipo;
    private final String titulo;
    private final String mensaje;

    EventoNotificacion(TipoNotificacion tipo, String titulo, String mensaje) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
    }

    public TipoNotificacion tipo() {
        return tipo;
    }

    public String titulo() {
        return titulo;
    }

    public String mensaje() {
        return mensaje;
    }
}
