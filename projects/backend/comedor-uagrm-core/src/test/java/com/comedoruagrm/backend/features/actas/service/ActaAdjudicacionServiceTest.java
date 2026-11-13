package com.comedoruagrm.backend.features.actas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.actas.dto.ActaResponse;
import com.comedoruagrm.backend.features.actas.model.ActaAdjudicacion;
import com.comedoruagrm.backend.features.actas.repository.ActaAdjudicacionRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.Adjudicado;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.actas.service.ActaAdjudicacionService.VistaPreviaActa;
import com.comedoruagrm.backend.features.actas.service.ActaPdfService.DatosActa;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Generar el acta y su vista previa (CU08) con los colaboradores simulados. */
// Escenario base compartido: cada test usa solo parte de lo que deja armado, por eso no es estricto.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActaAdjudicacionServiceTest {

    private static final long CONVOCATORIA_ID = 7L;
    private static final Instant AHORA = Instant.parse("2026-11-13T15:30:00Z");
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};

    @Mock
    private ActasConsultaRepository consulta;

    @Mock
    private ActaAdjudicacionRepository actaRepository;

    @Mock
    private NumeroActaService numeroActaService;

    @Mock
    private ActaPdfService pdfService;

    @Mock
    private AlmacenamientoService almacenamiento;

    @Mock
    private UsuarioService usuarioService;

    private ActaAdjudicacionService service;
    private Usuario admin;
    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new ActaAdjudicacionService(consulta, actaRepository, numeroActaService, pdfService,
                almacenamiento, usuarioService, Clock.fixed(AHORA, ZoneOffset.UTC));
        admin = new Usuario(null, "0000001", "admin@dubss.com", "hash", "Admin", "DUBSS");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "codigo", actorCodigo);
    }

    private List<Adjudicado> adjudicados(int cantidad) {
        return java.util.stream.IntStream.rangeClosed(1, cantidad)
                .mapToObj(i -> new Adjudicado("Nombre" + i, "Apellido" + i, "98000" + i, "Contaduría",
                        new BigDecimal("80.00")))
                .toList();
    }

    private void convocatoriaPublicadaSinActa(List<Adjudicado> adjudicados) {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.of(new DatosConvocatoria(CONVOCATORIA_ID,
                convocatoria, "Beca 2026-II", "PUBLICADA", "2026-II", Instant.parse("2026-07-01T12:00:00Z"),
                Instant.parse("2026-08-31T12:00:00Z"), 50, new BigDecimal("60.00"))));
        when(actaRepository.existsByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(false);
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.of(Instant.parse("2026-11-12T12:00:00Z")));
        when(consulta.adjudicadosDelActa(CONVOCATORIA_ID)).thenReturn(adjudicados);
    }

    private void generacionFeliz(List<Adjudicado> adjudicados) {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(adjudicados);
        when(numeroActaService.siguiente(AHORA)).thenReturn("001/2026");
        when(pdfService.generar(any(DatosActa.class), eq("001/2026"))).thenReturn(PDF);
        guardaElActa();
    }

    private void guardaElActa() {
        when(actaRepository.saveAndFlush(any(ActaAdjudicacion.class))).thenAnswer(i -> {
            ActaAdjudicacion a = i.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 50L);
            ReflectionTestUtils.setField(a, "codigo", UUID.fromString("00000000-0000-7000-8000-000000000050"));
            return a;
        });
    }

    // ---- generar: camino feliz ----

    @Test
    void generar_conResultadosPublicados_subeElPdf_guardaElActa_yDevuelveLaRespuestaSinRuta() {
        generacionFeliz(adjudicados(3));

        ResultadoAuditado<ActaResponse> resultado = service.generar(actorCodigo, convocatoria);

        String ruta = "actas/" + convocatoria + "/acta-001-2026.pdf";
        verify(almacenamiento).subir(ruta, PDF, "application/pdf");
        ArgumentCaptor<ActaAdjudicacion> guardada = ArgumentCaptor.forClass(ActaAdjudicacion.class);
        verify(actaRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getConvocatoriaId()).isEqualTo(CONVOCATORIA_ID);
        assertThat(guardada.getValue().getNumero()).isEqualTo("001/2026");
        assertThat(guardada.getValue().getStoragePath()).isEqualTo(ruta);
        assertThat(guardada.getValue().getGeneradaPor()).isEqualTo(1L);
        assertThat(guardada.getValue().getGeneradaEn()).isEqualTo(AHORA);

        ActaResponse respuesta = resultado.datos();
        assertThat(respuesta.numero()).isEqualTo("001/2026");
        assertThat(respuesta.convocatoria().codigo()).isEqualTo(convocatoria);
        assertThat(respuesta.convocatoria().nombre()).isEqualTo("Beca 2026-II");
        assertThat(respuesta.gestion()).isEqualTo("2026-II");
        assertThat(respuesta.adjudicados()).isEqualTo(3);
        assertThat(respuesta.generadaPor()).isEqualTo("Admin DUBSS");
        assertThat(respuesta.generadaEn()).isEqualTo(AHORA);
        assertThat(respuesta.toString()).doesNotContain("actas/");
        assertThat(resultado.actorId()).isEqualTo(1L);
        assertThat(resultado.entidadId()).isEqualTo(50L);
        assertThat(resultado.auditar()).isTrue();
    }

    @Test
    void generar_bloqueaLaConvocatoriaAntesDeValidar_yLeeLosAdjudicadosUnaSolaVez() {
        generacionFeliz(adjudicados(2));

        service.generar(actorCodigo, convocatoria);

        InOrder orden = inOrder(consulta, actaRepository, numeroActaService, pdfService, almacenamiento);
        orden.verify(consulta).bloquearConvocatoria(CONVOCATORIA_ID);
        orden.verify(actaRepository).existsByConvocatoriaId(CONVOCATORIA_ID);
        orden.verify(consulta).adjudicadosDelActa(CONVOCATORIA_ID);
        orden.verify(numeroActaService).siguiente(AHORA);
        orden.verify(pdfService).generar(any(DatosActa.class), eq("001/2026"));
        orden.verify(almacenamiento).subir(anyString(), any(byte[].class), eq("application/pdf"));
        orden.verify(actaRepository).saveAndFlush(any(ActaAdjudicacion.class));
        verify(consulta, times(1)).adjudicadosDelActa(CONVOCATORIA_ID);
    }

    @Test
    void generar_elPdfLlevaLosDatosDeLaConvocatoriaLosAdjudicadosYQuienLaGenera() {
        generacionFeliz(adjudicados(2));

        service.generar(actorCodigo, convocatoria);

        ArgumentCaptor<DatosActa> datos = ArgumentCaptor.forClass(DatosActa.class);
        verify(pdfService).generar(datos.capture(), eq("001/2026"));
        assertThat(datos.getValue().convocatoria()).isEqualTo("Beca 2026-II");
        assertThat(datos.getValue().gestion()).isEqualTo("2026-II");
        assertThat(datos.getValue().cupos()).isEqualTo(50);
        assertThat(datos.getValue().puntajeMinimo()).isEqualByComparingTo("60");
        assertThat(datos.getValue().adjudicados()).hasSize(2);
        assertThat(datos.getValue().generadaPor()).isEqualTo("Admin DUBSS");
        assertThat(datos.getValue().generadaEn()).isEqualTo(AHORA);
    }

    @Test
    void generar_sinNingunAdjudicado_generaIgual() {
        generacionFeliz(List.of());

        ActaResponse respuesta = service.generar(actorCodigo, convocatoria).datos();

        assertThat(respuesta.adjudicados()).isZero();
        verify(almacenamiento).subir(anyString(), any(byte[].class), eq("application/pdf"));
        verify(actaRepository).saveAndFlush(any(ActaAdjudicacion.class));
    }

    @Test
    void generar_elNumeroYLaRutaSiguenElFormato_tambienEnUnAnioNuevoYConMasDeTresDigitos() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(List.of());
        when(numeroActaService.siguiente(AHORA)).thenReturn("1000/2026");
        when(pdfService.generar(any(DatosActa.class), eq("1000/2026"))).thenReturn(PDF);
        guardaElActa();

        ActaResponse respuesta = service.generar(actorCodigo, convocatoria).datos();

        assertThat(respuesta.numero()).matches("[0-9]{3,}/[0-9]{4}");
        verify(almacenamiento).subir(eq("actas/" + convocatoria + "/acta-1000-2026.pdf"), any(byte[].class), anyString());
    }

    // ---- generar: errores de negocio ----

    @Test
    void generar_conResultadosSinPublicar_lanzaErrAct01_yNoGeneraNada() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(List.of());
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_01"));

        sinEfectos();
    }

    @Test
    void generar_siYaTieneActa_lanzaErrAct02_yNoGeneraNada() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(List.of());
        when(actaRepository.existsByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_02"));

        sinEfectos();
    }

    @Test
    void generar_yaConActaYSinPublicar_ganaErrAct02() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(List.of());
        when(actaRepository.existsByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(true);
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_02"));
    }

    @Test
    void generar_dosVeces_laPrimeraGenera_laSegundaRecibeErrAct02_yHayUnSoloArchivo() {
        generacionFeliz(adjudicados(1));

        service.generar(actorCodigo, convocatoria);
        // La segunda peticion, ya con el bloqueo tomado, ve el acta de la primera.
        when(actaRepository.existsByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_02"));

        verify(actaRepository, times(1)).saveAndFlush(any(ActaAdjudicacion.class));
        verify(almacenamiento, times(1)).subir(anyString(), any(byte[].class), anyString());
    }

    @Test
    void generar_siLaUnicidadDeLaBaseGanaLaCarrera_loConvierteEnErrAct02() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(List.of());
        when(numeroActaService.siguiente(AHORA)).thenReturn("001/2026");
        when(pdfService.generar(any(DatosActa.class), anyString())).thenReturn(PDF);
        when(actaRepository.saveAndFlush(any(ActaAdjudicacion.class)))
                .thenThrow(new DataIntegrityViolationException("uq_acta_adjudicacion_convocatoria"));

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_02"));
    }

    @Test
    void generar_otraViolacionDeIntegridad_subeSinConvertirse() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(List.of());
        when(numeroActaService.siguiente(AHORA)).thenReturn("001/2026");
        when(pdfService.generar(any(DatosActa.class), anyString())).thenReturn(PDF);
        when(actaRepository.saveAndFlush(any(ActaAdjudicacion.class)))
                .thenThrow(new DataIntegrityViolationException("uq_acta_adjudicacion_numero"));

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void generar_convocatoriaInexistente_lanzaNotFoundConErrPri01() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PRI_01"));

        verify(consulta, never()).bloquearConvocatoria(anyLong());
    }

    private void sinEfectos() {
        verify(numeroActaService, never()).siguiente(any());
        verifyNoInteractions(pdfService, almacenamiento);
        verify(actaRepository, never()).saveAndFlush(any(ActaAdjudicacion.class));
    }

    // ---- generar: fallas de infraestructura ----

    @Test
    void generar_siFallaMinio_laExcepcionSubeYNoSeGuardaNada() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(adjudicados(1));
        when(numeroActaService.siguiente(AHORA)).thenReturn("001/2026");
        when(pdfService.generar(any(DatosActa.class), anyString())).thenReturn(PDF);
        doThrow(new AlmacenamientoException("MinIO caído", null)).when(almacenamiento)
                .subir(anyString(), any(byte[].class), anyString());

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOf(AlmacenamientoException.class);

        verify(actaRepository, never()).saveAndFlush(any(ActaAdjudicacion.class));
    }

    @Test
    void generar_siFallaElPdf_laExcepcionSubeNoSeSubeNadaNiSeGuarda() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(adjudicados(1));
        when(numeroActaService.siguiente(AHORA)).thenReturn("001/2026");
        when(pdfService.generar(any(DatosActa.class), anyString())).thenThrow(new IllegalStateException("jasper"));

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(almacenamiento);
        verify(actaRepository, never()).saveAndFlush(any(ActaAdjudicacion.class));
    }

    @Test
    void generar_siFallaGuardarLaFila_laExcepcionSubeYNoHayResultadoParaLaBitacora() {
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        convocatoriaPublicadaSinActa(adjudicados(1));
        when(numeroActaService.siguiente(AHORA)).thenReturn("001/2026");
        when(pdfService.generar(any(DatosActa.class), anyString())).thenReturn(PDF);
        when(actaRepository.saveAndFlush(any(ActaAdjudicacion.class))).thenThrow(new IllegalStateException("base"));

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- bitacora, transaccion y permisos ----

    @Test
    void generar_laBitacoraLlevaCodigosNumeroYCantidad_peroNingunDatoSensible() {
        generacionFeliz(adjudicados(2));

        Map<String, Object> detalle = service.generar(actorCodigo, convocatoria).detalle();

        assertThat(detalle.keySet()).containsExactly("acta", "convocatoria", "numero", "adjudicados");
        assertThat(detalle).containsEntry("convocatoria", convocatoria.toString())
                .containsEntry("numero", "001/2026").containsEntry("adjudicados", 2);
        assertThat(detalle.toString()).doesNotContain("Nombre1", "Apellido1", "980001", "@", "admin", "DUBSS",
                "actas/", "http");
    }

    @Test
    void generar_esTransaccionalYAuditadoConLaAccionDeLaGeneracion_yExigeElRolAdministrador()
            throws NoSuchMethodException {
        var metodo = ActaAdjudicacionService.class.getMethod("generar", UUID.class, UUID.class);

        assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMINISTRADOR_DUBSS')");
        assertThat(metodo.getAnnotation(Auditar.class).accion()).isEqualTo(BitacoraAcciones.GENERACION_ACTA);
        assertThat(metodo.getAnnotation(Auditar.class).entidad()).isEqualTo("acta_adjudicacion");
        Transactional tx = metodo.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
        assertThat(tx.rollbackFor()).isEmpty();
        assertThat(tx.noRollbackFor()).isEmpty();
    }

    @Test
    void generar_noTocaPostulacionesNiAdjudicacionesNiAvisa() {
        assertThat(ActaAdjudicacionService.class.getDeclaredConstructors()).hasSize(1);
        assertThat(ActaAdjudicacionService.class.getDeclaredConstructors()[0].getParameterTypes())
                .extracting(Class::getSimpleName).containsExactly("ActasConsultaRepository",
                        "ActaAdjudicacionRepository", "NumeroActaService", "ActaPdfService", "AlmacenamientoService",
                        "UsuarioService", "Clock");
    }

    // ---- vista previa ----

    @Test
    void vistaPrevia_generaElBorrador_sinSubirGuardarNiConsumirNumero() {
        convocatoriaPublicadaSinActa(adjudicados(2));
        when(numeroActaService.borrador()).thenReturn("borrador");
        when(pdfService.generar(any(DatosActa.class), eq("borrador"))).thenReturn(PDF);

        VistaPreviaActa vista = service.vistaPrevia(convocatoria);

        assertThat(vista.contenido()).isEqualTo(PDF);
        assertThat(vista.nombreArchivo()).isEqualTo("acta-borrador-beca-2026-ii.pdf");
        verify(numeroActaService, never()).siguiente(any());
        verifyNoInteractions(almacenamiento, usuarioService);
        verify(actaRepository, never()).saveAndFlush(any(ActaAdjudicacion.class));
        verify(consulta, never()).bloquearConvocatoria(anyLong());
    }

    @Test
    void vistaPrevia_conActaExistente_lanzaErrAct02() {
        convocatoriaPublicadaSinActa(List.of());
        when(actaRepository.existsByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.vistaPrevia(convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_02"));

        verifyNoInteractions(pdfService, almacenamiento);
    }

    @Test
    void vistaPrevia_sinPublicar_lanzaErrAct01() {
        convocatoriaPublicadaSinActa(List.of());
        when(consulta.publicacion(CONVOCATORIA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.vistaPrevia(convocatoria))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_01"));

        verifyNoInteractions(pdfService, almacenamiento);
    }

    @Test
    void vistaPrevia_convocatoriaInexistente_lanzaErrPri01() {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.vistaPrevia(convocatoria))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PRI_01"));
    }

    @Test
    void vistaPrevia_esDeSoloLecturaExigeElRolYNoSeAnotaEnLaBitacora() throws NoSuchMethodException {
        var metodo = ActaAdjudicacionService.class.getMethod("vistaPrevia", UUID.class);

        assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMINISTRADOR_DUBSS')");
        assertThat(metodo.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(metodo.getAnnotation(Auditar.class)).isNull();
    }
}
