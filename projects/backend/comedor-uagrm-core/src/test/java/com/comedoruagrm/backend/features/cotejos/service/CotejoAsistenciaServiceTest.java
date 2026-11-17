package com.comedoruagrm.backend.features.cotejos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.cotejos.dto.CotejoResponse;
import com.comedoruagrm.backend.features.cotejos.dto.RegistrarCotejoRequest;
import com.comedoruagrm.backend.features.cotejos.enums.ResultadoCotejo;
import com.comedoruagrm.backend.features.cotejos.exception.CotejosErrorCodes;
import com.comedoruagrm.backend.features.cotejos.model.CotejoAsistencia;
import com.comedoruagrm.backend.features.cotejos.repository.CotejoAsistenciaRepository;
import com.comedoruagrm.backend.features.cotejos.repository.CotejosConsultaRepository;
import com.comedoruagrm.backend.features.cotejos.repository.CotejosConsultaRepository.DatosPlanilla;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Registrar el cotejo (CU11) con los colaboradores simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CotejoAsistenciaServiceTest {

    // 02:30 UTC del 17/11 es el 16/11 22:30 en La Paz: la fecha del cotejo es la de Bolivia.
    private static final Instant AHORA = Instant.parse("2026-11-17T02:30:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    @Mock
    private CotejosConsultaRepository consulta;

    @Mock
    private CotejoAsistenciaRepository cotejoRepository;

    @Mock
    private UsuarioService usuarioService;

    private CotejoAsistenciaService service;
    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID planilla = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new CotejoAsistenciaService(consulta, cotejoRepository, usuarioService,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        Usuario admin = new Usuario(null, "0000001", "admin@dubss.com", "hash", "Admin", "DUBSS");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "codigo", actorCodigo);
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        when(consulta.planilla(planilla)).thenReturn(Optional.of(new DatosPlanilla(9L, planilla, UUID.randomUUID(),
                "Beca 2026-II", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 880, AHORA)));
        when(cotejoRepository.saveAndFlush(any(CotejoAsistencia.class))).thenAnswer(i -> {
            CotejoAsistencia c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 70L);
            ReflectionTestUtils.setField(c, "codigo", UUID.fromString("00000000-0000-7000-8000-000000000070"));
            ReflectionTestUtils.setField(c, "creadoEn", AHORA);
            return c;
        });
    }

    private static RegistrarCotejoRequest pedido(int total, int coincidencias, int discrepancias,
            ResultadoCotejo resultado, String observaciones) {
        return new RegistrarCotejoRequest(total, coincidencias, discrepancias, resultado, observaciones);
    }

    private static void esErrorDe(Throwable e, String codigo) {
        assertThat(e).isInstanceOfSatisfying(ValidationException.class,
                v -> assertThat(v.getErrorCode()).isEqualTo(codigo));
    }

    @Test
    void registrar_conforme_guardaElCotejoConLaFechaDeBoliviaYElActor() {
        ResultadoAuditado<CotejoResponse> r = service.registrar(actorCodigo, planilla,
                pedido(880, 880, 0, ResultadoCotejo.CONFORME, "  "));

        ArgumentCaptor<CotejoAsistencia> guardado = ArgumentCaptor.forClass(CotejoAsistencia.class);
        verify(cotejoRepository).saveAndFlush(guardado.capture());
        CotejoAsistencia c = guardado.getValue();
        assertThat(c.getPlanillaId()).isEqualTo(9L);
        assertThat(c.getRealizadoPor()).isEqualTo(1L);
        assertThat(c.getFechaCotejo()).isEqualTo(HOY);
        assertThat(c.getObservaciones()).isNull();
        assertThat(r.datos().realizadoPor()).isEqualTo("Admin DUBSS");
        assertThat(r.datos().planilla()).isEqualTo(planilla);
        assertThat(r.actorId()).isEqualTo(1L);
        assertThat(r.entidadId()).isEqualTo(70L);
        assertThat(r.auditar()).isTrue();
    }

    @Test
    void registrar_conDiscrepanciasYObservaciones_laBitacoraNoLlevaLasObservaciones() {
        ResultadoAuditado<CotejoResponse> r = service.registrar(actorCodigo, planilla,
                pedido(880, 861, 19, ResultadoCotejo.CON_DISCREPANCIAS, "  Faltan firmas del 05/09  "));

        assertThat(r.datos().observaciones()).isEqualTo("Faltan firmas del 05/09");
        assertThat(r.detalle()).containsEntry("totalRegistros", 880).containsEntry("coincidencias", 861)
                .containsEntry("discrepancias", 19).containsEntry("resultado", "CON_DISCREPANCIAS");
        assertThat(r.detalle().toString()).doesNotContain("Faltan firmas");
        assertThat(r.detalle().values()).doesNotContain("Admin");
    }

    @Test
    void registrar_conCantidadesQueNoSuman_lanzaErrCot02YNoGuarda() {
        assertThatThrownBy(() -> service.registrar(actorCodigo, planilla,
                pedido(880, 800, 19, ResultadoCotejo.CON_DISCREPANCIAS, "x")))
                .satisfies(e -> esErrorDe(e, CotejosErrorCodes.ERR_COT_02));
        verify(cotejoRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_conResultadoQueNoCorrespondeALasDiscrepancias_lanzaErrCot03() {
        assertThatThrownBy(() -> service.registrar(actorCodigo, planilla,
                pedido(10, 8, 2, ResultadoCotejo.CONFORME, "x")))
                .satisfies(e -> esErrorDe(e, CotejosErrorCodes.ERR_COT_03));
        assertThatThrownBy(() -> service.registrar(actorCodigo, planilla,
                pedido(10, 10, 0, ResultadoCotejo.CON_DISCREPANCIAS, "x")))
                .satisfies(e -> esErrorDe(e, CotejosErrorCodes.ERR_COT_03));
        verify(cotejoRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_conDiscrepanciasSinObservaciones_lanzaErrCot04() {
        for (String vacia : new String[] {null, "", "   "}) {
            assertThatThrownBy(() -> service.registrar(actorCodigo, planilla,
                    pedido(10, 8, 2, ResultadoCotejo.CON_DISCREPANCIAS, vacia)))
                    .satisfies(e -> esErrorDe(e, CotejosErrorCodes.ERR_COT_04));
        }
        verify(cotejoRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_conPlanillaInexistente_lanzaNotFoundErrCot01() {
        UUID otra = UUID.randomUUID();
        when(consulta.planilla(otra)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(actorCodigo, otra,
                pedido(1, 1, 0, ResultadoCotejo.CONFORME, null)))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CotejosErrorCodes.ERR_COT_01));
    }

    @Test
    void registrar_conTotalCero_esConformeSiNoHayDiscrepancias() {
        ResultadoAuditado<CotejoResponse> r = service.registrar(actorCodigo, planilla,
                pedido(0, 0, 0, ResultadoCotejo.CONFORME, null));

        assertThat(r.datos().totalRegistros()).isZero();
    }

    @Test
    void registrar_exigeElRolAdministrador_esTransaccionalYSeAuditaConLaAccionDelCotejo()
            throws NoSuchMethodException {
        var metodo = CotejoAsistenciaService.class.getMethod("registrar", UUID.class, UUID.class,
                RegistrarCotejoRequest.class);

        assertThat(metodo.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMINISTRADOR_DUBSS')");
        assertThat(metodo.getAnnotation(Transactional.class)).isNotNull();
        assertThat(metodo.getAnnotation(Auditar.class).accion()).isEqualTo(BitacoraAcciones.COTEJO_ASISTENCIA);
    }
}
