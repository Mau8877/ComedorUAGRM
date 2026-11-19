package com.comedoruagrm.backend.features.contacto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.comedoruagrm.backend.features.contacto.dto.ContactoRequest;

@ExtendWith(MockitoExtension.class)
class ContactoServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void enviar_armaElCorreoConReplyToAsuntoFijoYDatosEnElCuerpo() {
        ContactoService servicio = new ContactoService(mailSender, "from@x.com", "dest@x.com");

        servicio.enviar(new ContactoRequest("Ana", "ana@mail.com", "Hola equipo", null));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage m = captor.getValue();
        assertThat(m.getTo()).containsExactly("dest@x.com");
        assertThat(m.getFrom()).isEqualTo("from@x.com");
        assertThat(m.getReplyTo()).isEqualTo("ana@mail.com");
        assertThat(m.getSubject()).doesNotContain("Ana");
        assertThat(m.getText()).contains("Ana").contains("ana@mail.com").contains("Hola equipo");
    }

    @Test
    void enviar_sinDestinoConfigurado_usaElRemitente() {
        ContactoService servicio = new ContactoService(mailSender, "from@x.com", " ");

        servicio.enviar(new ContactoRequest("Ana", "ana@mail.com", "Hola", ""));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("from@x.com");
    }

    @Test
    void enviar_conElCampoTrampaLleno_noEnviaNada() {
        ContactoService servicio = new ContactoService(mailSender, "from@x.com", "dest@x.com");

        servicio.enviar(new ContactoRequest("Bot", "bot@mail.com", "spam", "http://spam"));

        verifyNoInteractions(mailSender);
    }
}
