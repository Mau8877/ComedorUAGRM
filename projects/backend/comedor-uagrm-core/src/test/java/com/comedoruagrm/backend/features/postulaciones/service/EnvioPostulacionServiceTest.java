package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.ComprobantePostulacionResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.DatosComplementarios;
import com.comedoruagrm.backend.features.postulaciones.dto.EnviarPostulacionRequest;
import com.comedoruagrm.backend.features.postulaciones.enums.EstadoPostulacion;
import com.comedoruagrm.backend.features.postulaciones.exception.EnvioPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.exception.ValidacionPostulacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.model.Postulacion;
import com.comedoruagrm.backend.features.postulaciones.model.PostulacionHistorial;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Adjunto;
import com.comedoruagrm.backend.features.postulaciones.repository.DocumentosPostulacionConsultaRepository.Requerido;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.CabeceraEnvio;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.ConvocatoriaAbierta;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.Perfil;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.PostulacionActual;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.ProximaConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.repository.EnvioPostulacionConsultaRepository.Respuesta;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionHistorialRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.PostulacionRepository;
import com.comedoruagrm.backend.features.preseleccion.service.PrecalificacionService;

class EnvioPostulacionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-07-16T15:00:00Z");

    private final EnvioPostulacionConsultaRepository consulta = mock(EnvioPostulacionConsultaRepository.class);
    private final DocumentosPostulacionConsultaRepository documentos =
            mock(DocumentosPostulacionConsultaRepository.class);
    private final ValidacionPostulacionService validacion = mock(ValidacionPostulacionService.class);
    private final PostulacionRepository postulaciones = mock(PostulacionRepository.class);
    private final PostulacionHistorialRepository historial = mock(PostulacionHistorialRepository.class);
    private final PrecalificacionService precalificacion = mock(PrecalificacionService.class);
    private final EnvioPostulacionService service = new EnvioPostulacionService(consulta, documentos, validacion,
            postulaciones, historial, precalificacion, Clock.fixed(AHORA, ZoneOffset.UTC));

    private final UUID usuario = UUID.randomUUID();
    private final UUID postulacionCodigo = UUID.randomUUID();
    private final UUID convocatoriaCodigo = UUID.randomUUID();
    private final Perfil perfil = new Perfil(8L, "Ana", "Pérez", "1234567", "Ing. Informática");
    private final ConvocatoriaAbierta abierta = new ConvocatoriaAbierta(5L, convocatoriaCodigo, "Convocatoria 2026-II",
            "2026-II", AHORA.plusSeconds(86400));
    private final Postulacion postulacion = mock(Postulacion.class);

    @BeforeEach
    void preparar() {
        when(postulaciones.findById(40L)).thenReturn(Optional.of(postulacion));
        when(postulacion.getFechaEnvio()).thenReturn(AHORA);
        when(postulacion.getNumeroEnvios()).thenReturn((short) 1);
    }

    private static String codigoDe(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    private CabeceraEnvio cabecera(String estado, int numeroEnvios) {
        return new CabeceraEnvio(40L, postulacionCodigo, estado, numeroEnvios, null, 5L, convocatoriaCodigo,
                "Convocatoria 2026-II", "2026-II");
    }

    private void dadaLaPostulacion(String estado) {
        CabeceraEnvio c = cabecera(estado, 0);
        when(consulta.cabecera(postulacionCodigo, usuario)).thenReturn(Optional.of(c));
        when(consulta.estadoActual(40L)).thenReturn(Optional.of(estado));
        when(documentos.requeridos(5L)).thenReturn(List.of());
        when(documentos.adjuntos(40L)).thenReturn(List.of());
    }

    private static EnviarPostulacionRequest completo() {
        return new EnviarPostulacionRequest(new BigDecimal("1800.00"), 4, "Provincia", "Sin vivienda propia",
                "Hogar monoparental", new BigDecimal("72.50"), complementarios());
    }

    /** Lo obligatorio del formulario oficial de la DUBSS; el resto queda vacio. */
    private static DatosComplementarios complementarios() {
        return new DatosComplementarios("Solo de padre o madre", "Comerciante minorista", "De Bs. 2501 a Bs. 4000",
                false, null, null, null, "Alquiler", null, false, null, false, null, null);
    }

    private static EnviarPostulacionRequest conComplementarios(DatosComplementarios c) {
        return new EnviarPostulacionRequest(new BigDecimal("1800.00"), 4, "Provincia", "Sin vivienda propia",
                "Hogar monoparental", new BigDecimal("72.50"), c);
    }

    // ---- iniciar ----------------------------------------------------------------------------------------

    @Test
    void iniciar_sinPostulacionPrevia_creaElBorradorYLoMarcaComoCreado() {
        when(consulta.perfil(usuario)).thenReturn(Optional.of(perfil));
        when(consulta.convocatoriaAbierta(AHORA)).thenReturn(Optional.of(abierta));
        when(consulta.postulacionDe(8L, 5L)).thenReturn(Optional.empty());
        when(consulta.crearBorrador(8L, 5L)).thenReturn(
                new PostulacionActual(40L, postulacionCodigo, "borrador", 0, null));
        when(consulta.respuesta(40L)).thenReturn(Optional.empty());

        EnvioPostulacionService.Inicio inicio = service.iniciar(usuario);

        assertThat(inicio.creada()).isTrue();
        assertThat(inicio.formulario().postulacion()).isEqualTo(postulacionCodigo);
        assertThat(inicio.formulario().estado()).isEqualTo("borrador");
        assertThat(inicio.formulario().perfil().carrera()).isEqualTo("Ing. Informática");
        assertThat(inicio.formulario().convocatoria().gestion()).isEqualTo("2026-II");
        assertThat(inicio.formulario().opciones().procedencias()).contains("Provincia");
        assertThat(inicio.formulario().respuesta()).isNull();
        verify(validacion).validar(usuario, convocatoriaCodigo);
    }

    @Test
    void iniciar_conUnaDevuelta_laDevuelveConSusRespuestasYNoCreaOtra() {
        when(consulta.perfil(usuario)).thenReturn(Optional.of(perfil));
        when(consulta.convocatoriaAbierta(AHORA)).thenReturn(Optional.of(abierta));
        when(consulta.postulacionDe(8L, 5L)).thenReturn(
                Optional.of(new PostulacionActual(40L, postulacionCodigo, "devuelta", 1, AHORA)));
        when(consulta.respuesta(40L)).thenReturn(Optional.of(new Respuesta(new BigDecimal("1800.00"), 4,
                "Provincia", "Sin vivienda propia", "Hogar monoparental", new BigDecimal("72.50"))));

        EnvioPostulacionService.Inicio inicio = service.iniciar(usuario);

        assertThat(inicio.creada()).isFalse();
        assertThat(inicio.formulario().estado()).isEqualTo("devuelta");
        assertThat(inicio.formulario().respuesta().dependientes()).isEqualTo(4);
        verify(consulta, never()).crearBorrador(anyLong(), anyLong());
    }

    @Test
    void iniciar_sinConvocatoriaAbierta_lanzaErrEnv01ConLaFechaDeLaProxima() {
        when(consulta.perfil(usuario)).thenReturn(Optional.of(perfil));
        when(consulta.convocatoriaAbierta(AHORA)).thenReturn(Optional.empty());
        when(consulta.proximaConvocatoria(AHORA)).thenReturn(
                Optional.of(new ProximaConvocatoria("Convocatoria 2027-I", Instant.parse("2027-01-10T14:00:00Z"))));

        assertThatThrownBy(() -> service.iniciar(usuario)).isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_01))
                .hasMessageContaining("Convocatoria 2027-I").hasMessageContaining("10/01/2027");
        verify(consulta, never()).crearBorrador(anyLong(), anyLong());
    }

    @Test
    void iniciar_conUnaCuentaQueNoEsDePostulante_lanzaErrVal02() {
        when(consulta.perfil(usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.iniciar(usuario)).isInstanceOf(ForbiddenException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_02));
    }

    @Test
    void iniciar_conLaValidacionRechazada_propagaElMotivoYNoCreaNada() {
        when(consulta.perfil(usuario)).thenReturn(Optional.of(perfil));
        when(consulta.convocatoriaAbierta(AHORA)).thenReturn(Optional.of(abierta));
        when(validacion.validar(usuario, convocatoriaCodigo)).thenThrow(new ConflictException(
                ValidacionPostulacionErrorCodes.ERR_VAL_01, "Ya tienes una postulación vigente"));

        assertThatThrownBy(() -> service.iniciar(usuario)).isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_01));
        verify(consulta, never()).crearBorrador(anyLong(), anyLong());
    }

    @Test
    void iniciar_conDosSolicitudesSimultaneas_laSegundaRecibeErrVal01() {
        when(consulta.perfil(usuario)).thenReturn(Optional.of(perfil));
        when(consulta.convocatoriaAbierta(AHORA)).thenReturn(Optional.of(abierta));
        when(consulta.postulacionDe(8L, 5L)).thenReturn(Optional.empty());
        when(consulta.crearBorrador(8L, 5L)).thenThrow(new DataIntegrityViolationException("uq"));

        assertThatThrownBy(() -> service.iniciar(usuario)).isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_01));
    }

    // ---- enviar -----------------------------------------------------------------------------------------

    @Test
    void enviar_conTodoCompleto_guardaPasaAPendienteDejaHistorialYPideLaPrecalificacion() {
        dadaLaPostulacion("borrador");

        ComprobantePostulacionResponse c = service.enviar(usuario, postulacionCodigo, completo());

        assertThat(c.codigo()).isEqualTo(postulacionCodigo);
        assertThat(c.estado()).isEqualTo("pendiente");
        assertThat(c.fechaEnvio()).isEqualTo(AHORA);
        assertThat(c.reenvio()).isFalse();
        assertThat(c.gestion()).isEqualTo("2026-II");
        verify(consulta).bloquearPostulacion(40L);
        verify(validacion).validar(usuario, convocatoriaCodigo);
        ArgumentCaptor<Respuesta> respuesta = ArgumentCaptor.forClass(Respuesta.class);
        verify(consulta).guardarRespuesta(org.mockito.ArgumentMatchers.eq(40L), respuesta.capture());
        assertThat(respuesta.getValue().procedencia()).isEqualTo("Provincia");
        verify(postulacion).enviar(AHORA);
        ArgumentCaptor<PostulacionHistorial> fila = ArgumentCaptor.forClass(PostulacionHistorial.class);
        verify(historial).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getEstadoAnterior()).isEqualTo(EstadoPostulacion.borrador);
        assertThat(fila.getValue().getEstadoNuevo()).isEqualTo(EstadoPostulacion.pendiente);
        assertThat(fila.getValue().getUsuarioId()).isNull();
        verify(precalificacion).solicitar(40L);
    }

    @Test
    void enviar_unaPostulacionDevuelta_esUnReenvioYLoDejaEnElHistorial() {
        dadaLaPostulacion("devuelta");

        ComprobantePostulacionResponse c = service.enviar(usuario, postulacionCodigo, completo());

        assertThat(c.reenvio()).isTrue();
        ArgumentCaptor<PostulacionHistorial> fila = ArgumentCaptor.forClass(PostulacionHistorial.class);
        verify(historial).saveAndFlush(fila.capture());
        assertThat(fila.getValue().getEstadoAnterior()).isEqualTo(EstadoPostulacion.devuelta);
        assertThat(fila.getValue().getMotivo()).isEqualTo("Reenvío tras corregir");
    }

    @Test
    void enviar_repetido_devuelveElComprobanteYNoProcesaOtraVez() {
        CabeceraEnvio enviada = new CabeceraEnvio(40L, postulacionCodigo, "pendiente", 1, AHORA, 5L,
                convocatoriaCodigo, "Convocatoria 2026-II", "2026-II");
        when(consulta.cabecera(postulacionCodigo, usuario)).thenReturn(Optional.of(enviada));
        when(consulta.estadoActual(40L)).thenReturn(Optional.of("pendiente"));

        ComprobantePostulacionResponse c = service.enviar(usuario, postulacionCodigo, completo());

        assertThat(c.estado()).isEqualTo("pendiente");
        assertThat(c.fechaEnvio()).isEqualTo(AHORA);
        assertThat(c.reenvio()).isFalse();
        verify(validacion, never()).validar(any(), any());
        verify(consulta, never()).guardarRespuesta(anyLong(), any());
        verify(historial, never()).saveAndFlush(any());
        verify(precalificacion, never()).solicitar(anyLong());
    }

    @Test
    void enviar_conLaPostulacionYaCalificada_tambienEsIdempotente() {
        CabeceraEnvio enviada = new CabeceraEnvio(40L, postulacionCodigo, "calificada", 1, AHORA, 5L,
                convocatoriaCodigo, "Convocatoria 2026-II", "2026-II");
        when(consulta.cabecera(postulacionCodigo, usuario)).thenReturn(Optional.of(enviada));
        when(consulta.estadoActual(40L)).thenReturn(Optional.of("calificada"));

        assertThat(service.enviar(usuario, postulacionCodigo, completo()).estado()).isEqualTo("calificada");
        verify(historial, never()).saveAndFlush(any());
    }

    @Test
    void enviar_unaPostulacionYaResuelta_lanzaErrEnv03() {
        dadaLaPostulacion("adjudicada");

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, completo()))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_03));
    }

    @Test
    void enviar_unaPostulacionAjenaOInexistente_lanzaErrEnv02() {
        when(consulta.cabecera(postulacionCodigo, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, completo()))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_02));
    }

    @Test
    void enviar_sinCamposObligatorios_lanzaErrEnv04ConLaListaDeLoQueFalta() {
        dadaLaPostulacion("borrador");
        EnviarPostulacionRequest incompleto = new EnviarPostulacionRequest(null, 4, " ", "Sin vivienda propia",
                "Hogar monoparental", null);

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, incompleto))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_04))
                .hasMessageContaining("ingresos familiares").hasMessageContaining("procedencia")
                .hasMessageContaining("promedio académico");
        verify(historial, never()).saveAndFlush(any());
        verify(precalificacion, never()).solicitar(anyLong());
    }

    @Test
    void enviar_conCuerpoVacio_lanzaErrEnv04() {
        dadaLaPostulacion("borrador");

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_04));
    }

    @Test
    void enviar_faltandoUnDocumentoObligatorio_lanzaErrEnv04ConSuNombre() {
        dadaLaPostulacion("borrador");
        when(documentos.requeridos(5L)).thenReturn(List.of(
                new Requerido(1L, UUID.randomUUID(), "Carnet de identidad", null, true),
                new Requerido(2L, UUID.randomUUID(), "Certificado de ingresos", null, true),
                new Requerido(3L, UUID.randomUUID(), "Foto de la vivienda", null, false)));
        when(documentos.adjuntos(40L)).thenReturn(List.of(
                new Adjunto(UUID.randomUUID(), 1L, "ci.pdf", "application/pdf", 10L, AHORA)));

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, completo()))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_04))
                .hasMessageContaining("Certificado de ingresos")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("Carnet", "Foto"));
    }

    @Test
    void enviar_conUnDocumentoOpcionalSinAdjuntar_seEnvia() {
        dadaLaPostulacion("borrador");
        when(documentos.requeridos(5L)).thenReturn(List.of(
                new Requerido(3L, UUID.randomUUID(), "Foto de la vivienda", null, false)));

        assertThat(service.enviar(usuario, postulacionCodigo, completo()).estado()).isEqualTo("pendiente");
    }

    @Test
    void enviar_conValoresFueraDeRangoOOpcionInvalida_lanzaErrEnv05() {
        dadaLaPostulacion("borrador");
        EnviarPostulacionRequest invalido = new EnviarPostulacionRequest(new BigDecimal("-5"), 99, "Marte",
                "Sin vivienda propia", "Hogar monoparental", new BigDecimal("101"), complementarios());

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, invalido))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_05))
                .hasMessageContaining("ingresos").hasMessageContaining("dependientes")
                .hasMessageContaining("procedencia").hasMessageContaining("promedio");
        verify(precalificacion, never()).solicitar(anyLong());
    }

    @Test
    void enviar_conMasDeDosDecimalesEnElPromedio_lanzaErrEnv05() {
        dadaLaPostulacion("borrador");
        EnviarPostulacionRequest r = new EnviarPostulacionRequest(new BigDecimal("1800"), 4, "Provincia",
                "Sin vivienda propia", "Hogar monoparental", new BigDecimal("72.555"), complementarios());

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, r))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_05));
    }

    // ---- datos complementarios del formulario oficial -------------------------------------------------------

    @Test
    void enviar_sinLosDatosComplementarios_lanzaErrEnv04ConLoQueFalta() {
        dadaLaPostulacion("borrador");

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, conComplementarios(null)))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_04))
                .hasMessageContaining("de quién depende").hasMessageContaining("rango de ingresos")
                .hasMessageContaining("tenencia de la vivienda").hasMessageContaining("discapacidad");
        verify(consulta, never()).guardarComplementarios(anyLong(), any());
    }

    @Test
    void enviar_conHijosSinDecirCuantos_lanzaErrEnv04() {
        dadaLaPostulacion("borrador");
        DatosComplementarios c = new DatosComplementarios("Ambos padres", "Rentista", "Menor de Bs. 2500", true,
                null, null, null, "Herencia", null, false, null, false, null, null);

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, conComplementarios(c)))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_04))
                .hasMessageContaining("cuántos hijos");
    }

    @Test
    void enviar_conUnaOpcionComplementariaInvalida_lanzaErrEnv05() {
        dadaLaPostulacion("borrador");
        DatosComplementarios c = new DatosComplementarios("Un vecino", "Rentista", "Mucho", false, null, null,
                null, "Alquiler", null, false, null, false, null, null);

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, conComplementarios(c)))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_05))
                .hasMessageContaining("dependencia").hasMessageContaining("rango de ingresos");
    }

    @Test
    void enviar_conMasDeDiezFamiliares_lanzaErrEnv05() {
        dadaLaPostulacion("borrador");
        List<DatosComplementarios.MiembroGrupoFamiliar> once = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(n -> new DatosComplementarios.MiembroGrupoFamiliar("Familiar " + n, "Hermano", 10, null,
                        null)).toList();
        DatosComplementarios c = new DatosComplementarios("Ambos padres", "Rentista", "Menor de Bs. 2500", false,
                null, once, null, "Herencia", null, false, null, false, null, null);

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, conComplementarios(c)))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(EnvioPostulacionErrorCodes.ERR_ENV_05))
                .hasMessageContaining("hasta 10");
    }

    @Test
    void enviar_conTodoCompleto_guardaLosDatosComplementariosComoJsonYDescartaLasFilasVacias() {
        dadaLaPostulacion("borrador");
        DatosComplementarios c = new DatosComplementarios("  Ambos padres ", "Rentista", "Menor de Bs. 2500", true,
                "Más de 1", List.of(
                        new DatosComplementarios.MiembroGrupoFamiliar(" María ", "Madre", 45, null, null),
                        new DatosComplementarios.MiembroGrupoFamiliar("  ", null, null, null, null)),
                null, "Herencia", null, true, "  Beca de trabajo ", false, "Se ignora si es no", " ");

        service.enviar(usuario, postulacionCodigo, conComplementarios(c));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(consulta).guardarComplementarios(org.mockito.ArgumentMatchers.eq(40L), json.capture());
        assertThat(json.getValue()).contains("\"dependenciaEconomica\":\"Ambos padres\"")
                .contains("\"nombre\":\"María\"").contains("Beca de trabajo")
                .doesNotContain("Se ignora si es no").doesNotContain("\"nombre\":null");
        assertThat(ComplementariosJson.leer(json.getValue()).grupoFamiliar()).hasSize(1);
    }

    @Test
    void iniciar_conUnaDevuelta_precargaLosDatosComplementariosGuardados() {
        when(consulta.perfil(usuario)).thenReturn(Optional.of(perfil));
        when(consulta.convocatoriaAbierta(AHORA)).thenReturn(Optional.of(abierta));
        when(consulta.postulacionDe(8L, 5L)).thenReturn(
                Optional.of(new PostulacionActual(40L, postulacionCodigo, "devuelta", 1, AHORA)));
        when(consulta.respuesta(40L)).thenReturn(Optional.of(new Respuesta(new BigDecimal("1800.00"), 4,
                "Provincia", "Sin vivienda propia", "Hogar monoparental", new BigDecimal("72.50"))));
        when(consulta.complementarios(40L)).thenReturn(Optional.of(ComplementariosJson.escribir(complementarios())));

        var respuesta = service.iniciar(usuario).formulario().respuesta();

        assertThat(respuesta.complementarios().rangoIngresos()).isEqualTo("De Bs. 2501 a Bs. 4000");
        assertThat(respuesta.complementarios().tenenciaVivienda()).isEqualTo("Alquiler");
    }

    @Test
    void iniciar_ofreceLasOpcionesDelFormularioOficial() {
        when(consulta.perfil(usuario)).thenReturn(Optional.of(perfil));
        when(consulta.convocatoriaAbierta(AHORA)).thenReturn(Optional.of(abierta));
        when(consulta.postulacionDe(8L, 5L)).thenReturn(Optional.empty());
        when(consulta.crearBorrador(8L, 5L)).thenReturn(
                new PostulacionActual(40L, postulacionCodigo, "borrador", 0, null));
        when(consulta.respuesta(40L)).thenReturn(Optional.empty());

        var opciones = service.iniciar(usuario).formulario().opciones();

        assertThat(opciones.dependencias()).containsExactly("Independiente", "Solo de padre o madre", "Otro familiar",
                "De la pareja", "Ambos padres");
        assertThat(opciones.rangosIngresos()).hasSize(4);
        assertThat(opciones.tenenciasVivienda()).contains("Anticrético", "Alquiler");
    }

    @Test
    void enviar_conLaConvocatoriaCerrada_propagaErrVal03YNoGuardaNada() {
        dadaLaPostulacion("borrador");
        when(validacion.validar(usuario, convocatoriaCodigo)).thenThrow(new ConflictException(
                ValidacionPostulacionErrorCodes.ERR_VAL_03, "La convocatoria no está recibiendo postulaciones"));

        assertThatThrownBy(() -> service.enviar(usuario, postulacionCodigo, completo()))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo(ValidacionPostulacionErrorCodes.ERR_VAL_03));
        verify(consulta, never()).guardarRespuesta(anyLong(), any());
        verify(postulacion, never()).enviar(any());
    }
}
