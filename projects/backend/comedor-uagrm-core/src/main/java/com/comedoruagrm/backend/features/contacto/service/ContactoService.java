package com.comedoruagrm.backend.features.contacto.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.comedoruagrm.backend.features.contacto.dto.ContactoRequest;

/**
 * Reenvía por correo el mensaje del formulario de contacto de la landing.
 *
 * * Es síncrono a propósito: si el correo no sale, la persona tiene que enterarse (el
 * * error cae en el handler global como 500) en vez de ver un "enviado" falso.
 * * El asunto es fijo y los datos de la persona van solo en el cuerpo y en Reply-To
 * * (el correo ya validado como email, sin saltos de línea): evita inyección de cabeceras.
 * ! Ni el correo ni el mensaje se escriben en los logs.
 */
@Service
public class ContactoService {

    private static final Logger LOG = LoggerFactory.getLogger(ContactoService.class);

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String destino;

    public ContactoService(JavaMailSender mailSender,
            @Value("${app.mail.from}") String remitente,
            @Value("${app.contact.to:}") String destino) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        // Sin CONTACT_TO_EMAIL se usa el remitente: ya está verificado en SES.
        this.destino = destino == null || destino.isBlank() ? remitente : destino.trim();
    }

    public void enviar(ContactoRequest request) {
        if (request.sitioWeb() != null && !request.sitioWeb().isBlank()) {
            // Honeypot: se responde como si hubiera salido, sin enviar nada.
            LOG.warn("Mensaje de contacto descartado por el campo trampa", kv("module", "contacto"));
            return;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setReplyTo(request.correo().trim());
        mensaje.setSubject("Nuevo mensaje de contacto - Nexware");
        mensaje.setText("Nombre: " + request.nombre().trim() + "\n"
                + "Correo: " + request.correo().trim() + "\n\n"
                + request.mensaje().trim());
        mailSender.send(mensaje);
        LOG.info("Mensaje de contacto enviado", kv("module", "contacto"));
    }
}
