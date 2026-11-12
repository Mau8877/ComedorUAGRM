package com.comedoruagrm.backend.features.expedientes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.expedientes.dto.AccesoDocumentoResponse;
import com.comedoruagrm.backend.features.expedientes.dto.AuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.dto.ExpedienteResponse;
import com.comedoruagrm.backend.features.expedientes.dto.RegistrarAuditoriaRequest;
import com.comedoruagrm.backend.features.expedientes.dto.ResultadoAuditoriaResponse;
import com.comedoruagrm.backend.features.expedientes.exception.ExpedienteErrorCodes;
import com.comedoruagrm.backend.features.expedientes.model.AuditoriaExpediente;
import com.comedoruagrm.backend.features.expedientes.repository.AuditoriaExpedienteRepository;
import com.comedoruagrm.backend.features.expedientes.enums.ResultadoAuditoria;
import com.comedoruagrm.backend.features.expedientes.repository.ExpedienteConsultaRepository;
import com.comedoruagrm.backend.features.expedientes.repository.ExpedienteConsultaRepository.Cabecera;
import com.comedoruagrm.backend.features.finalistas.exception.FinalistaErrorCodes;
import com.comedoruagrm.backend.features.notificaciones.enums.EventoNotificacion;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

class ExpedienteServiceTest {

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

    private Cabecera cabecera(BigDecimal ia, String desglose, OffsetDateTime recibida) {
        return new Cabecera(1L, 2L, 3L, codigo, 4, null, recibida, "Ana", "Rojas", "123", "Sistemas",
                UUID.randomUUID(), "Conv", "calificada", ia, desglose);
    }

    private AuditoriaResponse auditoria(String academico) {
        return new AuditoriaResponse(UUID.randomUUID(), ResultadoAuditoria.VALIDADO, null,
                academico == null ? null : new BigDecimal(academico), Instant.now(), "Ana Rojas");
    }

    @Test
    void obtener_conFinalistaInexistente_lanzaNotFound() {
        when(consulta.cabecera(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(codigo)).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo(FinalistaErrorCodes.ERR_FIN_01);
    }

    @Test
    void obtener_conUltimaAuditoriaConPuntaje_sumaIaMasAcademico() {
        when(consulta.cabecera(codigo)).thenReturn(Optional.of(cabecera(new BigDecimal("55.50"),
                "{\"ingresos\": 20}", null)));
        when(consulta.auditorias(1L)).thenReturn(List.of(auditoria("25.00"), auditoria("10.00")));
        when(consulta.respuestaSocioeconomica(2L)).thenReturn(Optional.empty());

        ExpedienteResponse r = service.obtener(codigo);

        assertThat(r.puntajeAcademico()).isEqualByComparingTo("25.00");
        assertThat(r.puntajeTotal()).isEqualByComparingTo("80.50");
        assertThat(r.desglose()).containsKey("ingresos");
        assertThat(r.finalista().entrega().estado()).isEqualTo("PENDIENTE");
        assertThat(r.respuestaSocioeconomica()).isNull();
    }

    @Test
    void obtener_sinAuditoria_noCalculaTotal() {
        when(consulta.cabecera(codigo)).thenReturn(Optional.of(cabecera(new BigDecimal("55.50"), null,
                OffsetDateTime.parse("2026-11-06T15:00:00Z"))));
        when(consulta.auditorias(1L)).thenReturn(List.of());

        ExpedienteResponse r = service.obtener(codigo);

        assertThat(r.puntajeTotal()).isNull();
        assertThat(r.puntajeAcademico()).isNull();
        assertThat(r.desglose()).isNull();
        assertThat(r.finalista().entrega().estado()).isEqualTo("RECIBIDA");
        assertThat(r.finalista().entrega().fecha()).hasToString("2026-11-06");
    }

    @Test
    void total_sinPuntajeDeIaOSinAcademico_esNulo() {
        assertThat(ExpedienteService.total(null, new BigDecimal("20"))).isNull();
        assertThat(ExpedienteService.total(new BigDecimal("50"), null)).isNull();
    }

    @Test
    void obtener_conDesgloseInvalido_loDejaNulo() {
        when(consulta.cabecera(codigo)).thenReturn(Optional.of(cabecera(null, "no es json", null)));
        when(consulta.auditorias(1L)).thenReturn(List.of());

        assertThat(service.obtener(codigo).desglose()).isNull();
    }

    // ---- registrarAuditoria (BE-07) ----

    private static final OffsetDateTime RECIBIDA = OffsetDateTime.parse("2026-11-06T15:00:00Z");

    private void preparar(String estado, OffsetDateTime recibida) {
        Cabecera base = cabecera(new BigDecimal("50"), null, recibida);
        Cabecera c = new Cabecera(base.finalistaId(), base.postulacionId(), base.convocatoriaId(),
                base.finalistaCodigo(), base.posicion(), null, recibida, "Ana", "Perez", "123", "Sistemas",
                base.convocatoriaCodigo(), "Conv", estado, base.puntajeIa(), null);
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

    private void assertCodigo(Runnable accion, String esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(esperado));
    }

    @Test
    void registrarAuditoria_conFinalistaInexistente_lanzaErrFin01() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(consulta.cabecera(codigo)).thenReturn(Optional.empty());

        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null, validado("20")), "ERR_FIN_01");
    }

    @Test
    void registrarAuditoria_observadoSinObservaciones_lanzaErrExp03() {
        preparar("calificada", RECIBIDA);

        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null,
                new RegistrarAuditoriaRequest(ResultadoAuditoria.OBSERVADO, "   ", null)),
                ExpedienteErrorCodes.ERR_EXP_03);
        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null,
                new RegistrarAuditoriaRequest(ResultadoAuditoria.NO_VALIDO, null, null)),
                ExpedienteErrorCodes.ERR_EXP_03);
        verify(auditorias, never()).saveAndFlush(any());
    }

    @Test
    void registrarAuditoria_validadoSinPuntaje_oFueraDeRango_lanzaErrExp04() {
        preparar("calificada", RECIBIDA);

        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null, validado(null)),
                ExpedienteErrorCodes.ERR_EXP_04);
        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null, validado("30.01")),
                ExpedienteErrorCodes.ERR_EXP_04);
        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null, validado("-1")),
                ExpedienteErrorCodes.ERR_EXP_04);
        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null, validado("20.123")),
                ExpedienteErrorCodes.ERR_EXP_04);
    }

    @Test
    void registrarAuditoria_observadoConPuntajeFueraDeRango_tambienLanzaErrExp04() {
        preparar("calificada", RECIBIDA);

        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null,
                new RegistrarAuditoriaRequest(ResultadoAuditoria.OBSERVADO, "Falta carnet",
                        new BigDecimal("31"))), ExpedienteErrorCodes.ERR_EXP_04);
    }

    @Test
    void registrarAuditoria_conPostulacionNoCalificada_lanzaErrExp02() {
        preparar("devuelta", RECIBIDA);

        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null, validado("20")),
                ExpedienteErrorCodes.ERR_EXP_02);
    }

    // CU05: un finalista ya decidido (adjudicado o no adjudicado) deja de ser auditable.
    @Test
    void registrarAuditoria_conPostulacionAdjudicadaONoAdjudicada_lanzaErrExp02() {
        for (String estado : new String[] {"adjudicada", "no_adjudicada"}) {
            preparar(estado, RECIBIDA);

            assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null, validado("20")),
                    ExpedienteErrorCodes.ERR_EXP_02);
        }
    }

    @Test
    void registrarAuditoria_sinEntregaFisica_lanzaErrExp01() {
        preparar("calificada", null);

        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, null, validado("20")),
                ExpedienteErrorCodes.ERR_EXP_01);
    }

    @Test
    void registrarAuditoria_validada_guardaConElAuditorDelTokenYAvisaSinObservaciones() {
        preparar("calificada", RECIBIDA);

        ResultadoAuditado<ResultadoAuditoriaResponse> r = service.registrarAuditoria(actorCodigo, codigo, null,
                validado("25.50"));

        assertThat(r.datos().creada()).isTrue();
        assertThat(r.datos().auditoria().resultado()).isEqualTo(ResultadoAuditoria.VALIDADO);
        assertThat(r.datos().auditoria().auditor()).isEqualTo("Admin Perez");
        assertThat(r.auditar()).isTrue();
        assertThat(r.actorId()).isEqualTo(7L);
        assertThat(r.entidadId()).isEqualTo(99L);
        assertThat(r.detalle()).containsEntry("resultado", "VALIDADO").containsEntry("puntajeAcademico", "25.50")
                .doesNotContainKey("observaciones");
        ArgumentCaptor<AuditoriaExpediente> guardada = ArgumentCaptor.forClass(AuditoriaExpediente.class);
        verify(auditorias).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getAuditorId()).isEqualTo(7L);
        assertThat(guardada.getValue().getFinalistaId()).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> datos = ArgumentCaptor.forClass(Map.class);
        verify(notificaciones).notificar(eq(postulante), eq(EventoNotificacion.AUDITORIA_VALIDADA),
                datos.capture(), eq(2L), eq(null));
        assertThat(datos.getValue()).containsOnlyKeys("nombres");
    }

    @Test
    void registrarAuditoria_observadaYNoValida_eligenSuEventoYLlevanObservaciones() {
        preparar("calificada", RECIBIDA);

        service.registrarAuditoria(actorCodigo, codigo, null,
                new RegistrarAuditoriaRequest(ResultadoAuditoria.OBSERVADO, " Falta el carnet ", null));
        service.registrarAuditoria(actorCodigo, codigo, null,
                new RegistrarAuditoriaRequest(ResultadoAuditoria.NO_VALIDO, "Documento falso", null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> datos = ArgumentCaptor.forClass(Map.class);
        verify(notificaciones).notificar(eq(postulante), eq(EventoNotificacion.AUDITORIA_OBSERVADA),
                datos.capture(), eq(2L), eq(null));
        assertThat(datos.getValue()).containsEntry("observaciones", "Falta el carnet");
        verify(notificaciones).notificar(eq(postulante), eq(EventoNotificacion.AUDITORIA_NO_VALIDA),
                any(), eq(2L), eq(null));
    }

    @Test
    void registrarAuditoria_conAvisoFallido_oQueLanza_noDeshaceLaAuditoria() {
        preparar("calificada", RECIBIDA);
        when(notificaciones.notificar(any(), any(), any(), anyLong(), any())).thenReturn(false);

        assertThat(service.registrarAuditoria(actorCodigo, codigo, null, validado("20")).datos().creada()).isTrue();

        when(notificaciones.notificar(any(), any(), any(), anyLong(), any()))
                .thenThrow(new IllegalStateException("smtp"));
        assertThat(service.registrarAuditoria(actorCodigo, codigo, null, validado("21")).datos().creada()).isTrue();
    }

    private AuditoriaExpediente previa(UUID clave, ResultadoAuditoria resultado, String obs, String puntaje) {
        AuditoriaExpediente a = new AuditoriaExpediente(1L, 7L, Instant.now().minusSeconds(3600), resultado, obs,
                puntaje == null ? null : new BigDecimal(puntaje), clave);
        ReflectionTestUtils.setField(a, "id", 55L);
        ReflectionTestUtils.setField(a, "codigo", UUID.randomUUID());
        return a;
    }

    @Test
    void registrarAuditoria_conLaMismaClaveYContenido_devuelveLaGuardadaSinCrearAnotarNiAvisar() {
        preparar("calificada", RECIBIDA);
        UUID clave = UUID.randomUUID();
        AuditoriaExpediente previa = previa(clave, ResultadoAuditoria.VALIDADO, null, "20.00");
        when(auditorias.findByFinalistaIdAndClaveIdempotencia(1L, clave)).thenReturn(Optional.of(previa));
        AuditoriaResponse existente = new AuditoriaResponse(previa.getCodigo(), ResultadoAuditoria.VALIDADO, null,
                new BigDecimal("20.00"), Instant.now(), "Ana Rojas");
        when(consulta.auditorias(1L)).thenReturn(List.of(existente));

        ResultadoAuditado<ResultadoAuditoriaResponse> r = service.registrarAuditoria(actorCodigo, codigo,
                clave.toString(), validado("20"));

        assertThat(r.datos().creada()).isFalse();
        assertThat(r.datos().auditoria()).isSameAs(existente);
        assertThat(r.auditar()).isFalse();
        verify(consulta).bloquearFinalista(1L);
        verify(auditorias, never()).saveAndFlush(any());
        verify(notificaciones, never()).notificar(any(), any(), any(), anyLong(), any());
    }

    @Test
    void registrarAuditoria_repetidaAunqueLaPostulacionYaNoEsteCalificada_devuelveLaGuardada() {
        preparar("devuelta", RECIBIDA);
        UUID clave = UUID.randomUUID();
        AuditoriaExpediente previa = previa(clave, ResultadoAuditoria.VALIDADO, null, "20.00");
        when(auditorias.findByFinalistaIdAndClaveIdempotencia(1L, clave)).thenReturn(Optional.of(previa));
        when(consulta.auditorias(1L)).thenReturn(List.of(new AuditoriaResponse(previa.getCodigo(),
                ResultadoAuditoria.VALIDADO, null, new BigDecimal("20.00"), Instant.now(), "Ana Rojas")));

        assertThat(service.registrarAuditoria(actorCodigo, codigo, clave.toString(), validado("20"))
                .datos().creada()).isFalse();
    }

    @Test
    void registrarAuditoria_conLaMismaClaveYOtroContenido_lanzaErrExp06() {
        preparar("calificada", RECIBIDA);
        UUID clave = UUID.randomUUID();
        when(auditorias.findByFinalistaIdAndClaveIdempotencia(1L, clave)).thenReturn(Optional.of(
                previa(clave, ResultadoAuditoria.VALIDADO, null, "20.00")));

        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, clave.toString(), validado("21")),
                "ERR_EXP_06");
        verify(auditorias, never()).saveAndFlush(any());
    }

    @Test
    void registrarAuditoria_conUnaClaveNuevaOSinClave_creaOtraYGuardaLaClave() {
        preparar("calificada", RECIBIDA);
        UUID clave = UUID.randomUUID();
        when(auditorias.findByFinalistaIdAndClaveIdempotencia(1L, clave)).thenReturn(Optional.empty());

        assertThat(service.registrarAuditoria(actorCodigo, codigo, clave.toString(), validado("20"))
                .datos().creada()).isTrue();
        assertThat(service.registrarAuditoria(actorCodigo, codigo, null, validado("20")).datos().creada())
                .isTrue();
        assertThat(service.registrarAuditoria(actorCodigo, codigo, "  ", validado("20")).datos().creada())
                .isTrue();
        verify(auditorias, org.mockito.Mockito.times(3)).saveAndFlush(any());
    }

    @Test
    void registrarAuditoria_conUnaClaveQueNoEsUuid_lanzaErrExp07() {
        assertCodigo(() -> service.registrarAuditoria(actorCodigo, codigo, "no-es-uuid", validado("20")),
                "ERR_EXP_07");
        verify(auditorias, never()).saveAndFlush(any());
    }

    // ---- accesoDocumento (BE-09) ----

    @Test
    void accesoDocumento_propio_devuelveLaUrlTemporalSinLaRuta() {
        UUID documento = UUID.randomUUID();
        when(consulta.cabecera(codigo)).thenReturn(Optional.of(cabecera(null, null, null)));
        when(consulta.rutaDocumento(2L, documento)).thenReturn(Optional.of("postulaciones/2/carnet.pdf"));
        when(almacenamiento.urlTemporal("postulaciones/2/carnet.pdf", AlmacenamientoService.VIGENCIA_ACCESO))
                .thenReturn("http://localhost:9000/firmada");

        Instant antes = Instant.now();
        AccesoDocumentoResponse r = service.accesoDocumento(codigo, documento);

        assertThat(r.url()).isEqualTo("http://localhost:9000/firmada");
        assertThat(r.expiraEn()).isBetween(antes.plusSeconds(299), Instant.now().plusSeconds(301));
    }

    @Test
    void accesoDocumento_deOtroFinalistaOInexistente_lanzaErrExp05() {
        UUID documento = UUID.randomUUID();
        when(consulta.cabecera(codigo)).thenReturn(Optional.of(cabecera(null, null, null)));
        when(consulta.rutaDocumento(2L, documento)).thenReturn(Optional.empty());

        assertCodigo(() -> service.accesoDocumento(codigo, documento), ExpedienteErrorCodes.ERR_EXP_05);
        verify(almacenamiento, never()).urlTemporal(any(), any());
    }

    @Test
    void accesoDocumento_conFinalistaInexistente_lanzaErrFin01() {
        when(consulta.cabecera(codigo)).thenReturn(Optional.empty());

        assertCodigo(() -> service.accesoDocumento(codigo, UUID.randomUUID()), "ERR_FIN_01");
    }

    @Test
    void accesoDocumento_conMinioCaido_dejaSubirLaExcepcionDeInfraestructura() {
        UUID documento = UUID.randomUUID();
        when(consulta.cabecera(codigo)).thenReturn(Optional.of(cabecera(null, null, null)));
        when(consulta.rutaDocumento(2L, documento)).thenReturn(Optional.of("ruta"));
        when(almacenamiento.urlTemporal(any(), any())).thenThrow(new AlmacenamientoException("fallo", null));

        assertThatThrownBy(() -> service.accesoDocumento(codigo, documento))
                .isInstanceOf(AlmacenamientoException.class);
    }
}
