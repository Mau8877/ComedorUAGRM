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
            "Hola {nombres}, esta es una prueba del servicio de notificaciones de ComedorU.",
            true),

    /**
     * Aviso al finalista de la entrega de su documentacion fisica (CU21). Push y correo; el lugar,
     * el horario y los documentos viajan en el texto (no tienen columna en el diseno logico).
     */
    FINALISTA_NOTIFICADO(TipoNotificacion.INFO,
            "Entrega de documentación física - Beca Comedor",
            "Hola {nombres}, quedaste como finalista de la Beca Comedor. Debes entregar tu documentación "
                    + "física hasta el {plazo}. Lugar y horario: {lugar}. Documentos a presentar: {documentos}.",
            true),

    /** Comunicado de la DUBSS (CU21). Solo bandeja y push: sin correo, para no mandar correo masivo. */
    COMUNICADO(TipoNotificacion.INFO, "{titulo}", "{contenido}", false),

    /** Aviso al postulante de que su expediente fue validado (CU07). Solo bandeja y push. */
    AUDITORIA_VALIDADA(TipoNotificacion.SUCCESS,
            "Expediente validado - Beca Comedor",
            "Hola {nombres}, la DUBSS validó tu expediente. Quedas habilitado para la decisión de "
                    + "adjudicación de la Beca Comedor.",
            false),

    /** Aviso al postulante de que su expediente fue observado (CU07); las observaciones dicen que corregir. */
    AUDITORIA_OBSERVADA(TipoNotificacion.WARNING,
            "Tu expediente fue observado - Beca Comedor",
            "Hola {nombres}, la DUBSS observó tu expediente. Observaciones: {observaciones}",
            false),

    /** Aviso al postulante de que su expediente no fue valido (CU07); las observaciones dicen el motivo. */
    AUDITORIA_NO_VALIDA(TipoNotificacion.ERROR,
            "Tu expediente no fue válido - Beca Comedor",
            "Hola {nombres}, la DUBSS no validó tu expediente. Motivo: {observaciones}",
            false),

    /**
     * Aviso al postulante de que su postulacion fue devuelta para corregir (CU19); el motivo dice que
     * corregir. Bandeja, push y correo.
     */
    POSTULACION_DEVUELTA(TipoNotificacion.WARNING,
            "Tu postulación fue devuelta - Beca Comedor",
            "Hola {nombres}, la DUBSS devolvió tu postulación para que la corrijas. Observaciones: {observaciones}",
            true);

    private final TipoNotificacion tipo;
    private final String titulo;
    private final String mensaje;
    private final boolean enviaCorreo;

    EventoNotificacion(TipoNotificacion tipo, String titulo, String mensaje, boolean enviaCorreo) {
        this.enviaCorreo = enviaCorreo;
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

    /** Si ademas de la bandeja y el push el aviso sale por correo. */
    public boolean enviaCorreo() {
        return enviaCorreo;
    }
}
