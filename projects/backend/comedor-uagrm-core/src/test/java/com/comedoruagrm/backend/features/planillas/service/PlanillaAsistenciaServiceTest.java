package com.comedoruagrm.backend.features.planillas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.planillas.dto.PlanillaResponse;
import com.comedoruagrm.backend.features.planillas.dto.ResumenPlanillaResponse;
import com.comedoruagrm.backend.features.planillas.model.PlanillaAsistencia;
import com.comedoruagrm.backend.features.planillas.repository.PlanillaAsistenciaRepository;
import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository;
import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository.Becado;
import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.planillas.service.PlanillaAsistenciaService.VistaPreviaPlanilla;
import com.comedoruagrm.backend.features.planillas.service.PlanillaPdfService.DatosPlanilla;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Generar la planilla de asistencia y su vista previa (CU09) con los colaboradores simulados. */
// Escenario base compartido: cada test usa solo parte de lo que deja armado, por eso no es estricto.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanillaAsistenciaServiceTest {

    private static final long CONVOCATORIA_ID = 7L;
    private static final Instant AHORA = Instant.parse("2026-11-16T15:30:00Z");
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};
    private static final LocalDate GESTION_DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate GESTION_HASTA = LocalDate.of(2026, 12, 15);
    private static final LocalDate DESDE = LocalDate.of(2026, 9, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 9, 30);

    @Mock
    private PlanillasConsultaRepository consulta;

    @Mock
    private PlanillaAsistenciaRepository planillaRepository;

    @Mock
    private PlanillaPdfService pdfService;

    @Mock
    private AlmacenamientoService almacenamiento;

    @Mock
    private UsuarioService usuarioService;

    private PlanillaAsistenciaService service;
    private Usuario admin;
    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new PlanillaAsistenciaService(consulta, planillaRepository, pdfService, almacenamiento,
                usuarioService, Clock.fixed(AHORA, ZoneOffset.UTC));
        admin = new Usuario(null, "0000001", "admin@dubss.com", "hash", "Admin", "DUBSS");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "codigo", actorCodigo);
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
    }

    private List<Becado> becados(int cantidad) {
        return IntStream.rangeClosed(1, cantidad)
                .mapToObj(i -> new Becado("Nombre" + i, "Apellido" + i, "98000" + i, "Contaduría"))
                .toList();
    }

    private void convocatoriaConBecados(List<Becado> becados) {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.of(new DatosConvocatoria(CONVOCATORIA_ID,
                convocatoria, "Beca 2026-II", "2026-II", GESTION_DESDE, GESTION_HASTA)));
        when(consulta.becadosVigentes(CONVOCATORIA_ID)).thenReturn(becados);
        when(consulta.contarVigentes(CONVOCATORIA_ID)).thenReturn((long) becados.size());
    }

    private void generacionFeliz(List<Becado> becados) {
        convocatoriaConBecados(becados);
        when(pdfService.generar(any(DatosPlanilla.class))).thenReturn(PDF);
        when(planillaRepository.saveAndFlush(any(PlanillaAsistencia.class))).thenAnswer(i -> {
            PlanillaAsistencia p = i.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 60L);
            ReflectionTestUtils.setField(p, "codigo", UUID.fromString("00000000-0000-7000-8000-000000000060"));
            return p;
        });
    }

    private static void esErrorDe(Throwable e, Class<? extends RuntimeException> tipo, String codigo) {
        assertThat(e).isInstanceOf(tipo);
        String actual = e instanceof ValidationException v ? v.getErrorCode()
                : e instanceof ConflictException c ? c.getErrorCode() : ((NotFoundException) e).getErrorCode();
        assertThat(actual).isEqualTo(codigo);
    }

    // ---- generar: camino feliz ----

    @Test
    void generar_conBecadosVigentes_subeElPdf_guardaLaPlanilla_yDevuelveLaRespuestaSinRuta() {
        generacionFeliz(becados(3));

        ResultadoAuditado<PlanillaResponse> resultado = service.generar(actorCodigo, convocatoria, DESDE, HASTA);

        ArgumentCaptor<String> ruta = ArgumentCaptor.forClass(String.class);
        verify(almacenamiento).subir(ruta.capture(), eq(PDF), eq("application/pdf"));
        assertThat(ruta.getValue()).startsWith("planillas/" + convocatoria + "/planilla-2026-09-01-2026-09-30-")
                .endsWith(".pdf");
        ArgumentCaptor<PlanillaAsistencia> guardada = ArgumentCaptor.forClass(PlanillaAsistencia.class);
        verify(planillaRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getConvocatoriaId()).isEqualTo(CONVOCATORIA_ID);
        assertThat(guardada.getValue().getGeneradaPor()).isEqualTo(1L);
        assertThat(guardada.getValue().getFechaDesde()).isEqualTo(DESDE);
        assertThat(guardada.getValue().getFechaHasta()).isEqualTo(HASTA);
        assertThat(guardada.getValue().getTotalBecados()).isEqualTo(3);
        assertThat(guardada.getValue().getStoragePath()).isEqualTo(ruta.getValue());
        assertThat(guardada.getValue().getGeneradaEn()).isEqualTo(AHORA);

        PlanillaResponse respuesta = resultado.datos();
        assertThat(respuesta.convocatoria()).isEqualTo(convocatoria);
        assertThat(respuesta.fechaDesde()).isEqualTo(DESDE);
        assertThat(respuesta.fechaHasta()).isEqualTo(HASTA);
        assertThat(respuesta.totalBecados()).isEqualTo(3);
        assertThat(respuesta.generadaPor()).isEqualTo("Admin DUBSS");
        assertThat(respuesta.generadaEn()).isEqualTo(AHORA);
        assertThat(respuesta.toString()).doesNotContain("planillas/");
        assertThat(resultado.actorId()).isEqualTo(1L);
        assertThat(resultado.entidadId()).isEqualTo(60L);
        assertThat(resultado.auditar()).isTrue();
        // La bitacora lleva codigos, periodo y cantidad: nunca nombres, CI ni la ruta.
        assertThat(resultado.detalle()).containsEntry("becados", 3).containsEntry("desde", "2026-09-01")
                .containsEntry("hasta", "2026-09-30");
        assertThat(resultado.detalle().toString()).doesNotContain("planillas/", "98000", "Apellido");
    }

    @Test
    void generar_validaGeneraSubeYRecienEntoncesGuarda_leyendoLosBecadosUnaSolaVez() {
        generacionFeliz(becados(2));

        service.generar(actorCodigo, convocatoria, DESDE, HASTA);

        InOrder orden = inOrder(consulta, pdfService, almacenamiento, planillaRepository);
        orden.verify(consulta).becadosVigentes(CONVOCATORIA_ID);
        orden.verify(pdfService).generar(any(DatosPlanilla.class));
        orden.verify(almacenamiento).subir(anyString(), any(byte[].class), eq("application/pdf"));
        orden.verify(planillaRepository).saveAndFlush(any(PlanillaAsistencia.class));
        verify(consulta, org.mockito.Mockito.times(1)).becadosVigentes(CONVOCATORIA_ID);
    }

    @Test
    void generar_elPdfLlevaLaConvocatoriaElPeriodoLosBecadosYQuienLaGenera() {
        generacionFeliz(becados(2));

        service.generar(actorCodigo, convocatoria, DESDE, HASTA);

        ArgumentCaptor<DatosPlanilla> datos = ArgumentCaptor.forClass(DatosPlanilla.class);
        verify(pdfService).generar(datos.capture());
        assertThat(datos.getValue().convocatoria()).isEqualTo("Beca 2026-II");
        assertThat(datos.getValue().gestion()).isEqualTo("2026-II");
        assertThat(datos.getValue().desde()).isEqualTo(DESDE);
        assertThat(datos.getValue().hasta()).isEqualTo(HASTA);
        assertThat(datos.getValue().becados()).hasSize(2);
        assertThat(datos.getValue().generadaPor()).isEqualTo("Admin DUBSS");
        assertThat(datos.getValue().generadaEn()).isEqualTo(AHORA);
        assertThat(datos.getValue().borrador()).isFalse();
    }

    @Test
    void generar_elPeriodoPuedeSerUnSoloDia_yTocarLosLimitesDeLaGestion() {
        generacionFeliz(becados(1));

        assertThat(service.generar(actorCodigo, convocatoria, DESDE, DESDE).datos().fechaDesde()).isEqualTo(DESDE);
        service.generar(actorCodigo, convocatoria, GESTION_DESDE, GESTION_DESDE.plusDays(30));
        service.generar(actorCodigo, convocatoria, GESTION_HASTA.minusDays(30), GESTION_HASTA);

        verify(planillaRepository, org.mockito.Mockito.times(3)).saveAndFlush(any(PlanillaAsistencia.class));
    }

    @Test
    void generar_dosVecesElMismoPeriodo_creaDosPlanillasConArchivosDistintos() {
        generacionFeliz(becados(1));

        service.generar(actorCodigo, convocatoria, DESDE, HASTA);
        service.generar(actorCodigo, convocatoria, DESDE, HASTA);

        ArgumentCaptor<String> rutas = ArgumentCaptor.forClass(String.class);
        verify(almacenamiento, org.mockito.Mockito.times(2)).subir(rutas.capture(), any(byte[].class), anyString());
        assertThat(rutas.getAllValues()).doesNotHaveDuplicates();
    }

    // ---- generar: validacion del periodo (A1) ----

    @Test
    void generar_conFechaFinalAnterior_lanzaErrPla01_yNoGeneraNada() {
        generacionFeliz(becados(1));

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria, HASTA, DESDE))
                .satisfies(e -> esErrorDe(e, ValidationException.class, "ERR_PLA_01"));

        sinEfectos();
    }

    @Test
    void generar_conPeriodoFueraDeLaGestion_lanzaErrPla02_yNoGeneraNada() {
        generacionFeliz(becados(1));

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria, GESTION_DESDE.minusDays(1), DESDE))
                .satisfies(e -> esErrorDe(e, ValidationException.class, "ERR_PLA_02"));
        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria, DESDE, GESTION_HASTA.plusDays(1)))
                .satisfies(e -> esErrorDe(e, ValidationException.class, "ERR_PLA_02"));

        sinEfectos();
    }

    @Test
    void generar_conMasDeTreintaYUnDias_lanzaErrPla03() {
        generacionFeliz(becados(1));

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria, DESDE, DESDE.plusDays(31)))
                .satisfies(e -> esErrorDe(e, ValidationException.class, "ERR_PLA_03"));
        // 31 dias exactos si entran.
        service.generar(actorCodigo, convocatoria, DESDE, DESDE.plusDays(30));
    }

    @Test
    void generar_conFechaFinalAnteriorYFueraDeGestion_ganaErrPla01() {
        generacionFeliz(becados(1));

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria, GESTION_HASTA.plusDays(5), DESDE))
                .satisfies(e -> esErrorDe(e, ValidationException.class, "ERR_PLA_01"));
    }

    // ---- generar: sin becas (A2) y convocatoria inexistente ----

    @Test
    void generar_sinBecasVigentes_lanzaErrPla04_yNoGeneraNada() {
        generacionFeliz(List.of());

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria, DESDE, HASTA))
                .satisfies(e -> esErrorDe(e, ConflictException.class, "ERR_PLA_04"));

        sinEfectos();
    }

    @Test
    void generar_convocatoriaInexistente_lanzaNotFoundConErrPri01() {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria, DESDE, HASTA))
                .satisfies(e -> esErrorDe(e, NotFoundException.class, "ERR_PRI_01"));

        verify(consulta, never()).becadosVigentes(anyLong());
    }

    private void sinEfectos() {
        verifyNoInteractions(pdfService, almacenamiento);
        verify(planillaRepository, never()).saveAndFlush(any(PlanillaAsistencia.class));
    }

    // ---- generar: fallas de infraestructura (A3) ----

    @Test
    void generar_siFallaMinio_laExcepcionSubeYNoSeRegistraLaPlanilla() {
        generacionFeliz(becados(1));
        doThrow(new AlmacenamientoException("sin conexión", null)).when(almacenamiento)
                .subir(anyString(), any(byte[].class), anyString());

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria, DESDE, HASTA))
                .isInstanceOf(AlmacenamientoException.class);

        verify(planillaRepository, never()).saveAndFlush(any(PlanillaAsistencia.class));
    }

    @Test
    void generar_siFallaElPdf_laExcepcionSubeYNoSeSubeNiSeRegistraNada() {
        generacionFeliz(becados(1));
        when(pdfService.generar(any(DatosPlanilla.class))).thenThrow(new IllegalStateException("jasper"));

        assertThatThrownBy(() -> service.generar(actorCodigo, convocatoria, DESDE, HASTA))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(almacenamiento);
        verify(planillaRepository, never()).saveAndFlush(any(PlanillaAsistencia.class));
    }

    // ---- vista previa ----

    @Test
    void vistaPrevia_devuelveElPdfEnBorrador_sinSubirNiGuardarNada() {
        convocatoriaConBecados(becados(2));
        when(pdfService.generar(any(DatosPlanilla.class))).thenReturn(PDF);

        VistaPreviaPlanilla vista = service.vistaPrevia(convocatoria, DESDE, HASTA);

        assertThat(vista.contenido()).isEqualTo(PDF);
        assertThat(vista.nombreArchivo()).isEqualTo("planilla-borrador-beca-2026-ii.pdf");
        ArgumentCaptor<DatosPlanilla> datos = ArgumentCaptor.forClass(DatosPlanilla.class);
        verify(pdfService).generar(datos.capture());
        assertThat(datos.getValue().borrador()).isTrue();
        assertThat(datos.getValue().generadaPor()).isEqualTo("—");
        verifyNoInteractions(almacenamiento, planillaRepository, usuarioService);
    }

    @Test
    void vistaPrevia_validaElPeriodoYLosBecadosComoGenerar() {
        convocatoriaConBecados(becados(1));
        assertThatThrownBy(() -> service.vistaPrevia(convocatoria, HASTA, DESDE))
                .satisfies(e -> esErrorDe(e, ValidationException.class, "ERR_PLA_01"));

        convocatoriaConBecados(List.of());
        assertThatThrownBy(() -> service.vistaPrevia(convocatoria, DESDE, HASTA))
                .satisfies(e -> esErrorDe(e, ConflictException.class, "ERR_PLA_04"));

        verifyNoInteractions(pdfService);
    }

    // ---- resumen ----

    @Test
    void resumen_devuelveLosBecadosVigentesYLosLimitesDeLaGestion() {
        convocatoriaConBecados(becados(5));

        ResumenPlanillaResponse resumen = service.resumen(convocatoria);

        assertThat(resumen.becadosVigentes()).isEqualTo(5);
        assertThat(resumen.gestion()).isEqualTo("2026-II");
        assertThat(resumen.gestionDesde()).isEqualTo(GESTION_DESDE);
        assertThat(resumen.gestionHasta()).isEqualTo(GESTION_HASTA);
    }

    @Test
    void resumen_convocatoriaInexistente_lanzaErrPri01() {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resumen(convocatoria))
                .satisfies(e -> esErrorDe(e, NotFoundException.class, "ERR_PRI_01"));
    }

    // ---- seguridad, transaccion y bitacora ----

    @Test
    void losMetodosPublicosExigenElRolAdministradorDubss() throws NoSuchMethodException {
        for (String nombre : List.of("generar", "vistaPrevia", "resumen")) {
            var metodo = java.util.Arrays.stream(PlanillaAsistenciaService.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals(nombre)).findFirst().orElseThrow();
            assertThat(metodo.getAnnotation(PreAuthorize.class).value())
                    .isEqualTo("hasRole('ADMINISTRADOR_DUBSS')");
        }
    }

    @Test
    void generar_esTransaccionalYSeAnotaEnLaBitacoraComoGeneracionPlanilla() {
        var metodo = java.util.Arrays.stream(PlanillaAsistenciaService.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("generar")).findFirst().orElseThrow();

        assertThat(metodo.getAnnotation(Transactional.class)).isNotNull();
        Auditar auditar = metodo.getAnnotation(Auditar.class);
        assertThat(auditar.accion()).isEqualTo(BitacoraAcciones.GENERACION_PLANILLA);
        assertThat(auditar.entidad()).isEqualTo("planilla_asistencia");
    }
}
