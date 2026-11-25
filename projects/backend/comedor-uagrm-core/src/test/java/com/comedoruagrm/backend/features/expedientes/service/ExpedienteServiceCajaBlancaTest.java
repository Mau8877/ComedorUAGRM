package com.comedoruagrm.backend.features.expedientes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.expedientes.dto.AuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.dto.RegistrarAuditoriaRequest;
import com.comedoruagrm.backend.features.expedientes.dto.ResultadoAuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;
import com.comedoruagrm.backend.features.expedientes.exception.ExpedienteErrorCodes;
import com.comedoruagrm.backend.features.expedientes.model.AuditoriaExpediente;
import com.comedoruagrm.backend.features.expedientes.repository.AuditoriaExpedienteRepository;
import com.comedoruagrm.backend.features.expedientes.repository.ExpedienteConsultaRepository;
import com.comedoruagrm.backend.features.expedientes.repository.ExpedienteConsultaRepository.Cabecera;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/**
 * Pruebas de caja blanca de CU-07 (capitulo 9, 9.3.2): un test por camino basico de
 * `ExpedienteService.registrarAuditoria` en el analisis acotado (C1 a C9).
 */
class ExpedienteServiceCajaBlancaTest {

    private static final OffsetDateTime RECIBIDA = OffsetDateTime.parse("2026-11-06T15:00:00Z");

    private final ExpedienteConsultaRepository consulta = mock(ExpedienteConsultaRepository.class);
    private final AuditoriaExpedienteRepository auditorias = mock(AuditoriaExpedienteRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final NotificacionService notificaciones = mock(NotificacionService.class);
    private final AlmacenamientoService almacenamiento = mock(AlmacenamientoService.class);
    private final ExpedienteService service = new ExpedienteService(consulta, auditorias, usuarios,
            notificaciones, almacenamiento);

    private final UUID codigo = UUID.randomUUID();
    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID postulanteCodigo = UUID.randomUUID();
    private final Usuario actor = usuario(7L, "Admin");
    private final Usuario postulante = usuario(8L, "Ana");

    private static Usuario usuario(long id, String nombres) {
        Usuario u = new Usuario(null, "1234" + id, nombres + "@uagrm.edu.bo", "hash", nombres, "Perez");
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "codigo", UUID.randomUUID());
        return u;
    }

    private void preparar(String estado, OffsetDateTime recibida) {
        Cabecera c = new Cabecera(1L, 2L, 3L, codigo, 4, null, recibida, "Ana", "Perez", "123", "Sistemas",
                UUID.randomUUID(), "Conv", estado, new BigDecimal("50"), null);
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(usuarios.obtenerPorCodigo(postulanteCodigo)).thenReturn(postulante);
        when(consulta.cabecera(codigo)).thenReturn(Optional.of(c));
        when(consulta.codigoPostulante(2L)).thenReturn(Optional.of(postulanteCodigo));
        when(auditorias.saveAndFlush(any(AuditoriaExpediente.class))).thenAnswer(i -> {
            AuditoriaExpediente a = i.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 99L);
            ReflectionTestUtils.setField(a, "codigo", UUID.randomUUID());
            return a;
        });
        when(notificaciones.notificar(any(), any(), any(), anyLong(), any())).thenReturn(true);
    }

    private static RegistrarAuditoriaRequest validado(String puntaje) {
        return new RegistrarAuditoriaRequest(ResultadoAuditoria.VALIDADO, null,
                puntaje == null ? null : new BigDecimal(puntaje));
    }

    private void falla(RegistrarAuditoriaRequest request, String clave, String esperado) {
        assertThatThrownBy(() -> service.registrarAuditoria(actorCodigo, codigo, clave, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(esperado));
        verify(auditorias, never()).saveAndFlush(any());
    }

    private AuditoriaExpediente previa(UUID clave) {
        AuditoriaExpediente a = new AuditoriaExpediente(1L, 7L, Instant.now().minusSeconds(3600),
                ResultadoAuditoria.VALIDADO, null, new BigDecimal("20.00"), clave);
        ReflectionTestUtils.setField(a, "id", 55L);
        ReflectionTestUtils.setField(a, "codigo", UUID.randomUUID());
        return a;
    }

    @Test
    void registrarAuditoria_C1_repeticionConLaMismaClaveYContenido_devuelveLaAuditoriaExistente() {
        preparar("calificada", RECIBIDA);
        UUID clave = UUID.randomUUID();
        AuditoriaExpediente previa = previa(clave);
        when(auditorias.findByFinalistaIdAndClaveIdempotencia(1L, clave)).thenReturn(Optional.of(previa));
        AuditoriaResponse existente = new AuditoriaResponse(previa.getCodigo(), ResultadoAuditoria.VALIDADO, null,
                new BigDecimal("20.00"), Instant.now(), "Ana Rojas");
        when(consulta.auditorias(1L)).thenReturn(List.of(existente));

        ResultadoAuditado<ResultadoAuditoriaResponse> r = service.registrarAuditoria(actorCodigo, codigo,
                clave.toString(), validado("20"));

        assertThat(r.datos().creada()).isFalse();
        assertThat(r.datos().auditoria()).isSameAs(existente);
        verify(auditorias, never()).saveAndFlush(any());
    }

    @Test
    void registrarAuditoria_C2_laMismaClaveConOtroContenido_lanzaErrExp06() {
        preparar("calificada", RECIBIDA);
        UUID clave = UUID.randomUUID();
        when(auditorias.findByFinalistaIdAndClaveIdempotencia(1L, clave)).thenReturn(Optional.of(previa(clave)));

        falla(validado("25"), clave.toString(), ExpedienteErrorCodes.ERR_EXP_06);
    }

    @Test
    void registrarAuditoria_C3_noValidadoSinObservaciones_lanzaErrExp03() {
        preparar("calificada", RECIBIDA);

        falla(new RegistrarAuditoriaRequest(ResultadoAuditoria.OBSERVADO, "   ", null), null,
                ExpedienteErrorCodes.ERR_EXP_03);
    }

    @Test
    void registrarAuditoria_C4_validadoSinPuntaje_lanzaErrExp04() {
        preparar("calificada", RECIBIDA);

        falla(validado(null), null, ExpedienteErrorCodes.ERR_EXP_04);
    }

    @Test
    void registrarAuditoria_C5_puntajeInvalido_lanzaErrExp04() {
        preparar("calificada", RECIBIDA);

        falla(validado("30.01"), null, ExpedienteErrorCodes.ERR_EXP_04);
    }

    @Test
    void registrarAuditoria_C6_postulacionNoCalificada_lanzaErrExp02() {
        preparar("devuelta", RECIBIDA);

        falla(validado("20"), null, ExpedienteErrorCodes.ERR_EXP_02);
    }

    @Test
    void registrarAuditoria_C7_sinRecepcionDeLaDocumentacion_lanzaErrExp01() {
        preparar("calificada", null);

        falla(validado("20"), null, ExpedienteErrorCodes.ERR_EXP_01);
    }

    @Test
    void registrarAuditoria_C8_conPuntaje_guardaAvisaYElDetalleLlevaElPuntaje() {
        preparar("calificada", RECIBIDA);

        ResultadoAuditado<ResultadoAuditoriaResponse> r = service.registrarAuditoria(actorCodigo, codigo, null,
                validado("25.50"));

        assertThat(r.datos().creada()).isTrue();
        assertThat(r.detalle()).containsEntry("puntajeAcademico", "25.50");
        verify(auditorias).saveAndFlush(any(AuditoriaExpediente.class));
        verify(notificaciones).notificar(any(), any(), any(), anyLong(), any());
    }

    @Test
    void registrarAuditoria_C9_sinPuntaje_guardaAvisaYElDetalleNoLlevaPuntaje() {
        preparar("calificada", RECIBIDA);

        ResultadoAuditado<ResultadoAuditoriaResponse> r = service.registrarAuditoria(actorCodigo, codigo, null,
                new RegistrarAuditoriaRequest(ResultadoAuditoria.OBSERVADO, "Falta el carnet", null));

        assertThat(r.datos().creada()).isTrue();
        assertThat(r.detalle()).doesNotContainKey("puntajeAcademico");
        verify(auditorias).saveAndFlush(any(AuditoriaExpediente.class));
        verify(notificaciones).notificar(any(), any(), any(), anyLong(), any());
    }
}
