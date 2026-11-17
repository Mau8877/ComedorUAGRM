package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.features.postulaciones.dto.BorradorGuardadoResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.BorradorResumenResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios;
import com.comedoruagrm.backend.features.postulaciones.dto.GuardarBorradorRequest;
import com.comedoruagrm.backend.features.postulaciones.exception.EnvioPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.exception.ValidacionPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Adjunto;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Requerido;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.BorradorAbierto;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.CabeceraEnvio;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.Respuesta;

class BorradorPostulacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-07-16T15:00:00Z");

    private final EnvioPostulacionConsultaRepository consulta = mock(EnvioPostulacionConsultaRepository.class);
    private final DocumentosPostulacionConsultaRepository documentos =
            mock(DocumentosPostulacionConsultaRepository.class);
    private final ValidacionPostulacionService validacion = mock(ValidacionPostulacionService.class);
    private final BorradorPostulacionService service = new BorradorPostulacionService(consulta, documentos,
            validacion, Clock.fixed(AHORA, ZoneOffset.UTC));

    private final UUID usuario = UUID.randomUUID();
    private final UUID postulacionCodigo = UUID.randomUUID();
    private final UUID convocatoriaCodigo = UUID.randomUUID();

    private static String codigoDe(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    private void dadaLaPostulacion(String estado) {
        when(consulta.cabecera(postulacionCodigo, usuario)).thenReturn(Optional.of(new CabeceraEnvio(40L,
                postulacionCodigo, estado, 0, null, 5L, convocatoriaCodigo, "Convocatoria 2026-II", "2026-II")));
        when(consulta.estadoActual(40L)).thenReturn(Optional.of(estado));
    }

    // ---- guardar ----------------------------------------------------------------------------------------

    @Test
    void guardar_conElFormularioParcial_guardaSinExigirLoObligatorioYDevuelveLoQueFalta() {
        dadaLaPostulacion("borrador");
        Instant guardado = AHORA.minusSeconds(5);
        when(consulta.guardadoEn(40L)).thenReturn(Optional.of(guardado));

        BorradorGuardadoResponse r = service.guardar(usuario, postulacionCodigo, new GuardarBorradorRequest(
                new BigDecimal("1800.00"), 4, "  Provincia ", "  ", null, null));

        ArgumentCaptor<Respuesta> guardada = ArgumentCaptor.forClass(Respuesta.class);
        verify(consulta).guardarRespuesta(org.mockito.ArgumentMatchers.eq(40L), guardada.capture());
        assertThat(guardada.getValue().procedencia()).isEqualTo("Provincia");
        assertThat(guardada.getValue().patrimonio()).isNull();
        assertThat(r.postulacion()).isEqualTo(postulacionCodigo);
        assertThat(r.guardadoEn()).isEqualTo(guardado);
        // * 3 de las columnas (patrimonio, vulnerabilidad, promedio) + 7 obligatorios del formulario oficial.
        assertThat(r.camposPendientes()).isEqualTo(10);
        verify(consulta).bloquearPostulacion(40L);
        verify(validacion).validar(usuario, convocatoriaCodigo);
    }

    @Test
    void guardar_conElCuerpoVacio_guardaTodoVacioYTodoPendiente() {
        dadaLaPostulacion("borrador");

        BorradorGuardadoResponse r = service.guardar(usuario, postulacionCodigo,
                new GuardarBorradorRequest(null, null, null, null, null, null));

        assertThat(r.camposPendientes()).isEqualTo(13);
        assertThat(r.guardadoEn()).isEqualTo(AHORA);
        verify(consulta).guardarComplementarios(40L, null);
    }

    @Test
    void guardar_conDatosComplementarios_losGuardaComoJsonYDescuentaLosPendientes() {
        dadaLaPostulacion("borrador");
        DatosComplementarios c = new DatosComplementarios("Ambos padres", " ", "Menor de Bs. 2500", false, null,
                null, null, null, null, null, null, null, null, "Estudio y trabajo");

        BorradorGuardadoResponse r = service.guardar(usuario, postulacionCodigo, new GuardarBorradorRequest(
                null, null, null, null, null, null, c));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(consulta).guardarComplementarios(org.mockito.ArgumentMatchers.eq(40L), json.capture());
        assertThat(json.getValue()).contains("\"dependenciaEconomica\":\"Ambos padres\"")
                .contains("Estudio y trabajo");
        // * 6 de columnas + ocupación, tenencia, otro beneficio y discapacidad (las otras tres ya están).
        assertThat(r.camposPendientes()).isEqualTo(10);
    }

    @Test
    void guardar_unaOpcionComplementariaInvalida_rechazaConErrEnv05SinGuardar() {
        dadaLaPostulacion("borrador");
        DatosComplementarios c = new DatosComplementarios("Un vecino", null, null, null, null, null, null, null,
                null, null, null, null, null, null);

        assertThatThrownBy(() -> service.guardar(usuario, postulacionCodigo, new GuardarBorradorRequest(
                null, null, null, null, null, null, c)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_05))
                .hasMessageContaining("dependencia económica");
        verify(consulta, never()).guardarRespuesta(anyLong(), any());
        verify(consulta, never()).guardarComplementarios(anyLong(), any());
    }

    @Test
    void guardar_unaDevuelta_tambienSePuedeGuardar() {
        dadaLaPostulacion("devuelta");

        service.guardar(usuario, postulacionCodigo, new GuardarBorradorRequest(null, 2, null, null, null, null));

        verify(consulta).guardarRespuesta(anyLong(), any());
    }

    @Test
    void guardar_unValorInvalido_rechazaTodoConErrEnv05SinGuardar() {
        dadaLaPostulacion("borrador");

        assertThatThrownBy(() -> service.guardar(usuario, postulacionCodigo, new GuardarBorradorRequest(
                new BigDecimal("1800"), 4, "Marte", null, null, new BigDecimal("120"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_05))
                .hasMessageContaining("procedencia").hasMessageContaining("promedio");
        verify(consulta, never()).guardarRespuesta(anyLong(), any());
    }

    @Test
    void guardar_laConvocatoriaCerro_rechazaConErrVal03SinGuardar() {
        dadaLaPostulacion("borrador");
        doThrow(new ConflictException(ValidacionPostulacionErrorCodes.ERR_VAL_03, "El plazo finalizó"))
                .when(validacion).validar(usuario, convocatoriaCodigo);

        assertThatThrownBy(() -> service.guardar(usuario, postulacionCodigo,
                new GuardarBorradorRequest(null, null, null, null, null, null)))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_03));
        verify(consulta, never()).guardarRespuesta(anyLong(), any());
    }

    @Test
    void guardar_unaPostulacionYaEnviada_rechazaConErrEnv03() {
        dadaLaPostulacion("pendiente");

        assertThatThrownBy(() -> service.guardar(usuario, postulacionCodigo,
                new GuardarBorradorRequest(null, null, null, null, null, null)))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_03));
        verify(consulta, never()).guardarRespuesta(anyLong(), any());
    }

    @Test
    void guardar_unaPostulacionAjenaOInexistente_da404ErrEnv02() {
        when(consulta.cabecera(postulacionCodigo, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.guardar(usuario, postulacionCodigo,
                new GuardarBorradorRequest(null, null, null, null, null, null)))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_02));
        verify(consulta, never()).bloquearPostulacion(anyLong());
    }

    // ---- obtener ----------------------------------------------------------------------------------------

    @Test
    void obtener_sinBorrador_devuelveVacio() {
        when(consulta.borradorAbierto(usuario, AHORA)).thenReturn(Optional.empty());

        assertThat(service.obtener(usuario)).isEmpty();
    }

    @Test
    void obtener_conBorrador_cuentaLosCamposYLosDocumentosObligatoriosQueFaltan() {
        when(consulta.borradorAbierto(usuario, AHORA)).thenReturn(Optional.of(new BorradorAbierto(40L,
                postulacionCodigo, 5L, "Convocatoria 2026-II", "2026-II", AHORA.plusSeconds(86400))));
        when(consulta.respuesta(40L)).thenReturn(Optional.of(new Respuesta(new BigDecimal("1800.00"), 4,
                "Provincia", null, null, null)));
        when(consulta.guardadoEn(40L)).thenReturn(Optional.of(AHORA.minusSeconds(60)));
        when(documentos.requeridos(5L)).thenReturn(List.of(requerido(1L, "Cédula", true),
                requerido(2L, "Certificado", true), requerido(3L, "Foto", false)));
        when(documentos.adjuntos(40L)).thenReturn(List.of(adjunto(1L)));

        BorradorResumenResponse r = service.obtener(usuario).orElseThrow();

        assertThat(r.postulacion()).isEqualTo(postulacionCodigo);
        assertThat(r.convocatoria()).isEqualTo("Convocatoria 2026-II");
        assertThat(r.camposPendientes()).isEqualTo(10);
        assertThat(r.documentosPendientes()).isEqualTo(1);
        assertThat(r.guardadoEn()).isEqualTo(AHORA.minusSeconds(60));
    }

    @Test
    void obtener_sinNingunAvanceGuardado_todoPendienteYSinFechaDeGuardado() {
        when(consulta.borradorAbierto(usuario, AHORA)).thenReturn(Optional.of(new BorradorAbierto(40L,
                postulacionCodigo, 5L, "Convocatoria 2026-II", "2026-II", AHORA.plusSeconds(86400))));
        when(consulta.respuesta(40L)).thenReturn(Optional.empty());
        when(documentos.requeridos(5L)).thenReturn(List.of());
        when(documentos.adjuntos(40L)).thenReturn(List.of());

        BorradorResumenResponse r = service.obtener(usuario).orElseThrow();

        assertThat(r.camposPendientes()).isEqualTo(13);
        assertThat(r.guardadoEn()).isNull();
    }

    private static Requerido requerido(long tipo, String nombre, boolean obligatorio) {
        return new Requerido(tipo, UUID.randomUUID(), nombre, null, obligatorio);
    }

    private static Adjunto adjunto(long tipo) {
        return new Adjunto(UUID.randomUUID(), tipo, "archivo.pdf", "application/pdf", 10L, AHORA);
    }
}
