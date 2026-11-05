package com.comedoruagrm.backend.features.auth.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.comedoruagrm.backend.config.CorreoConfig;

/**
 * Envia el correo con el enlace para restablecer la contraseña (CU15).
 *
 * * Es asincrono (ejecutor propio): quien lo llama no espera al SMTP, y el
 * * tiempo de respuesta de "recuperar contraseña" no delata si la cuenta existe.
 * * Un fallo del servicio de correo se registra y NO se propaga (A9, CA18): la
 * * persona ve igual el mensaje neutro y puede volver a pedir el enlace.
 * ! Ni el correo, ni el token, ni el enlace se escriben en los logs.
 */
@Service
public class CorreoRestablecimientoService {

    private static final Logger LOG = LoggerFactory.getLogger(CorreoRestablecimientoService.class);

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String urlBase;
    private final long vigenciaMinutos;

    public CorreoRestablecimientoService(JavaMailSender mailSender,
            @Value("${app.mail.from}") String remitente,
            @Value("${app.web.base-url}") String urlBase,
            @Value("${app.auth.reset-ttl-minutes}") long vigenciaMinutos) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.vigenciaMinutos = vigenciaMinutos;
    }

    @Async(CorreoConfig.EJECUTOR_CORREO)
    public void enviarEnlace(String correoDestino, String nombres, String token) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(correoDestino);
            mensaje.setSubject("Restablece tu contraseña de ComedorU");
            mensaje.setText(cuerpo(nombres, token));
            mailSender.send(mensaje);
        } catch (RuntimeException e) {
            // Solo el tipo de la excepcion: su mensaje puede traer el destinatario.
            LOG.warn("No se pudo enviar el correo de restablecimiento",
                    kv("module", "auth"), kv("causa", e.getClass().getSimpleName()));
        }
    }

    private String cuerpo(String nombres, String token) {
        String enlace = urlBase + "/restablecer-contrasena?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        return "Hola " + nombres + ",\n\n"
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta de ComedorU.\n"
                + "Abre este enlace para definir una contraseña nueva:\n\n"
                + enlace + "\n\n"
                + "El enlace es de un solo uso y vence en " + vigenciaMinutos + " minutos.\n\n"
                + "Si no lo pediste tú, ignora este correo: tu contraseña no cambia.\n\n"
                + "Dirección de Bienestar Social y Salud (DUBSS) - UAGRM";
    }
}
