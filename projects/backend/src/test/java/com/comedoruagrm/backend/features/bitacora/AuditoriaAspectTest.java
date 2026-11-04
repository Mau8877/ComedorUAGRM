package com.comedoruagrm.backend.features.bitacora;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.comedoruagrm.backend.common.auditoria.Auditable;
import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.features.bitacora.service.BitacoraService;

class AuditoriaAspectTest {

    private static final UUID CODIGO = UUID.randomUUID();

    private final BitacoraService bitacoraService = mock(BitacoraService.class);
    private final Servicio servicio = conAspecto(new Servicio());

    @AfterEach
    void limpiarRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void metodoAnotado_queTerminaBien_anotaLaAccionConElActorYLaIpDelRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        servicio.hacer(true);

        verify(bitacoraService).registrar(eq(7L), eq(CODIGO), eq("ACCION_X"), eq("usuario"), eq(7L),
                eq(Map.of("rol", "POSTULANTE")), eq("203.0.113.10"));
    }

    @Test
    void metodoAnotado_fueraDeUnRequest_anotaSinIp() {
        servicio.hacer(true);

        verify(bitacoraService).registrar(eq(7L), eq(CODIGO), eq("ACCION_X"), eq("usuario"), eq(7L),
                any(), eq((String) null));
    }

    @Test
    void metodoAnotado_cuandoElResultadoDiceQueNoAuditar_noAnotaNada() {
        servicio.hacer(false);

        verifyNoInteractions(bitacoraService);
    }

    @Test
    void metodoAnotado_queLanzaUnaExcepcion_noAnotaNada() {
        assertThatThrownBy(servicio::fallar).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(bitacoraService);
    }

    @Test
    void metodoSinLaAnotacion_noAnotaNada() {
        servicio.sinAnotar();

        verifyNoInteractions(bitacoraService);
    }

    @Test
    void metodoAnotado_queNoDevuelveUnAuditable_noAnotaNada() {
        servicio.devolverTexto();

        verifyNoInteractions(bitacoraService);
    }

    @Test
    void llamadaInternaDeLaMismaClase_noSeAudita() {
        servicio.llamarInternamente();

        verifyNoInteractions(bitacoraService);
    }

    private Servicio conAspecto(Servicio objetivo) {
        AspectJProxyFactory fabrica = new AspectJProxyFactory(objetivo);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new AuditoriaAspect(bitacoraService));
        return fabrica.getProxy();
    }

    /** Servicio de prueba con los casos que el aspecto tiene que distinguir. */
    public static class Servicio {

        @Auditar(accion = "ACCION_X", entidad = "usuario")
        public Resultado hacer(boolean auditar) {
            return new Resultado(auditar);
        }

        @Auditar(accion = "ACCION_X", entidad = "usuario")
        public Resultado fallar() {
            throw new IllegalStateException("fallo");
        }

        public Resultado sinAnotar() {
            return new Resultado(true);
        }

        @Auditar(accion = "ACCION_X", entidad = "usuario")
        public String devolverTexto() {
            return "no es auditable";
        }

        public Resultado llamarInternamente() {
            return interno();
        }

        @Auditar(accion = "ACCION_X", entidad = "usuario")
        public Resultado interno() {
            return new Resultado(true);
        }
    }

    public record Resultado(boolean auditar) implements Auditable {

        @Override
        public Long actorId() {
            return 7L;
        }

        @Override
        public UUID actorCodigo() {
            return CODIGO;
        }

        @Override
        public Map<String, Object> detalle() {
            return Map.of("rol", "POSTULANTE");
        }
    }
}
