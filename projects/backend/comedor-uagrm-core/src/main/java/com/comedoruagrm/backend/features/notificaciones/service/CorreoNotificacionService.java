package com.comedoruagrm.backend.features.notificaciones.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Correo de un aviso (CU-EXTRA-01). Con reintentos, y nunca propaga un fallo.
 *
 * * Es distinto de `CorreoRestablecimientoService` (CU15): aquel lleva un enlace de un solo uso
 * * y no pasa por la bandeja; este envia el mismo texto del aviso.
 * * No es `@Async` a proposito: ya lo llama `DespachoNotificaciones`, que corre en el ejecutor
 * * de notificaciones; otro salto asincrono con espera podria agotar los hilos de ese ejecutor.
 * * Devuelve si se entrego, para que quien despacha sepa si marcar el aviso como enviado.
 * ! Ni el destinatario ni el texto se escriben en los logs.
 */
@Service
public class CorreoNotificacionService {

    private static final Logger LOG = LoggerFactory.getLogger(CorreoNotificacionService.class);

    private static final String FIRMA = "\n\nDirección de Bienestar Social y Salud (DUBSS) - UAGRM";

    private final JavaMailSender mailSender;
    private final String remitente;
    private final Reintentos reintentos;

    public CorreoNotificacionService(JavaMailSender mailSender, @Value("${app.mail.from}") String remitente,
            Reintentos reintentos) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.reintentos = reintentos;
    }

    /** Envia el correo; devuelve true si se entrego y false si se agotaron los intentos. */
    public boolean enviar(String destino, String asunto, String texto) {
        try {
            reintentos.ejecutar(() -> {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setFrom(remitente);
                mensaje.setTo(destino);
                mensaje.setSubject(asunto);
                mensaje.setText(texto + FIRMA);
                mailSender.send(mensaje);
                return Boolean.TRUE;
            }, e -> e instanceof MailSendException);
            return true;
        } catch (RuntimeException e) {
            // Solo el tipo de la excepcion: su mensaje puede traer el destinatario.
            LOG.warn("No se pudo enviar el correo del aviso", kv("module", "notificaciones"),
                    kv("causa", e.getClass().getSimpleName()));
            return false;
        }
    }
}
