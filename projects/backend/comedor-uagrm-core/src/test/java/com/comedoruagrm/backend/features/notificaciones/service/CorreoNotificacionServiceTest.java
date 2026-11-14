package com.comedoruagrm.backend.features.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class CorreoNotificacionServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private CorreoNotificacionService servicio() {
        return new CorreoNotificacionService(mailSender, "no-responder@comedoru.local",
                new Reintentos(3, new long[] {0}));
    }

    @Test
    void enviar_conSmtpDisponible_enviaElCorreoConAsuntoTextoYFirma() {
        boolean entregado = servicio().enviar("ana@uagrm.edu.bo", "Asunto", "Texto del aviso");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(entregado).isTrue();
        assertThat(captor.getValue().getTo()).containsExactly("ana@uagrm.edu.bo");
        assertThat(captor.getValue().getFrom()).isEqualTo("no-responder@comedoru.local");
        assertThat(captor.getValue().getSubject()).isEqualTo("Asunto");
        assertThat(captor.getValue().getText()).startsWith("Texto del aviso").contains("DUBSS");
    }

    @Test
    void enviar_siElSmtpFallaUnaVezYLuegoFunciona_reintentaYEntrega() {
        doThrow(new MailSendException("caido")).doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        boolean entregado = servicio().enviar("ana@uagrm.edu.bo", "Asunto", "Texto");

        assertThat(entregado).isTrue();
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviar_siElSmtpSiempreFalla_agotaLosIntentosDevuelveFalseYNoLanza() {
        doThrow(new MailSendException("caido")).when(mailSender).send(any(SimpleMailMessage.class));

        boolean entregado = servicio().enviar("ana@uagrm.edu.bo", "Asunto", "Texto");

        assertThat(entregado).isFalse();
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviar_conUnErrorQueNoEsDeTransporte_noReintenta() {
        doThrow(new MailParseException("direccion mal formada")).when(mailSender).send(any(SimpleMailMessage.class));

        boolean entregado = servicio().enviar("no-es-correo", "Asunto", "Texto");

        assertThat(entregado).isFalse();
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
