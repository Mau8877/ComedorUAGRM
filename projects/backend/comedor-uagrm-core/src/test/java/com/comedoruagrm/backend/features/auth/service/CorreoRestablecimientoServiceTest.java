package com.comedoruagrm.backend.features.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class CorreoRestablecimientoServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private CorreoRestablecimientoService servicio;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void preparar() {
        servicio = new CorreoRestablecimientoService(mailSender, "no-responder@comedoru.local",
                "http://localhost/", 60);
        logger = (Logger) LoggerFactory.getLogger(CorreoRestablecimientoService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void limpiar() {
        logger.detachAppender(logs);
    }

    @Test
    void enviarEnlace_armaElMensajeConDestinatarioRemitenteAsuntoYEnlace() {
        servicio.enviarEnlace("ana@uagrm.edu.bo", "Ana", "tok-123_abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensaje = captor.getValue();
        assertThat(mensaje.getTo()).containsExactly("ana@uagrm.edu.bo");
        assertThat(mensaje.getFrom()).isEqualTo("no-responder@comedoru.local");
        assertThat(mensaje.getSubject()).isEqualTo("Restablece tu contraseña de ComedorU");
        assertThat(mensaje.getText())
                .contains("Hola Ana")
                .contains("http://localhost/restablecer-contrasena?token=tok-123_abc")
                .contains("60 minutos");
    }

    @Test
    void enviarEnlace_conUnNombreConCaracteresEspeciales_noRompeElCuerpo() {
        servicio.enviarEnlace("jose@uagrm.edu.bo", "José Ñandú <b>&\"", "tok");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("Hola José Ñandú <b>&\"");
    }

    @Test
    void enviarEnlace_codificaElTokenEnLaUrl() {
        servicio.enviarEnlace("ana@uagrm.edu.bo", "Ana", "a b&c");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("?token=a+b%26c");
    }

    @Test
    void enviarEnlace_siElSmtpFalla_noPropagaYLogueaSinElCorreoNiElToken() {
        doThrow(new MailSendException("fallo con ana@uagrm.edu.bo")).when(mailSender).send(any(SimpleMailMessage.class));

        servicio.enviarEnlace("ana@uagrm.edu.bo", "Ana", "token-secreto");

        assertThat(logs.list).hasSize(1);
        String texto = logs.list.get(0).getFormattedMessage() + logs.list.get(0).getArgumentArray()[0];
        assertThat(texto).doesNotContain("ana@uagrm.edu.bo").doesNotContain("token-secreto");
        assertThat(logs.list.get(0).getLevel().toString()).isEqualTo("WARN");
    }
}
