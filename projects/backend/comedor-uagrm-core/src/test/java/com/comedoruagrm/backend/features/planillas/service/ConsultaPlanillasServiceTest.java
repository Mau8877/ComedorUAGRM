package com.comedoruagrm.backend.features.planillas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.planillas.dto.AccesoPlanillaResponse;
import com.comedoruagrm.backend.features.planillas.dto.PlanillaResponse;
import com.comedoruagrm.backend.features.planillas.model.PlanillaAsistencia;
import com.comedoruagrm.backend.features.planillas.repository.PlanillaAsistenciaRepository;
import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository;
import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Listar y descargar las planillas de asistencia (CU09, flujo B) con los colaboradores simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultaPlanillasServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T15:30:00Z");
    private static final String RUTA = "planillas/x/planilla-2026-09-01-2026-09-30-abc.pdf";

    @Mock
    private PlanillasConsultaRepository consulta;

    @Mock
    private PlanillaAsistenciaRepository planillaRepository;

    @Mock
    private AlmacenamientoService almacenamiento;

    @Mock
    private UsuarioService usuarioService;

    private ConsultaPlanillasService service;
    private Usuario admin;
    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();
    private final UUID planillaCodigo = UUID.randomUUID();
    private PlanillaAsistencia planilla;

    @BeforeEach
    void preparar() {
        service = new ConsultaPlanillasService(consulta, planillaRepository, almacenamiento, usuarioService,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        admin = new Usuario(null, "0000001", "admin@dubss.com", "hash", "Admin", "DUBSS");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "codigo", actorCodigo);
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
        planilla = new PlanillaAsistencia(7L, 1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 3, RUTA, AHORA);
        ReflectionTestUtils.setField(planilla, "id", 60L);
        ReflectionTestUtils.setField(planilla, "codigo", planillaCodigo);
        when(planillaRepository.findByCodigo(planillaCodigo)).thenReturn(Optional.of(planilla));
    }

    private void convocatoriaExiste() {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.of(new DatosConvocatoria(7L, convocatoria,
                "Beca 2026-II", "2026-II", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15))));
    }

    @Test
    void listar_devuelveLaPaginaDeLaConvocatoria() {
        convocatoriaExiste();
        Pageable pagina = PageRequest.of(0, 20);
        Page<PlanillaResponse> esperada = new PageImpl<>(List.of(new PlanillaResponse(planillaCodigo, convocatoria,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 3, AHORA, "Admin DUBSS")), pagina, 1);
        when(consulta.planillas(convocatoria, pagina)).thenReturn(esperada);

        assertThat(service.listar(convocatoria, pagina)).isSameAs(esperada);
    }

    @Test
    void listar_convocatoriaInexistente_lanzaErrPri01() {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(convocatoria, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PRI_01"));

        verify(consulta, never()).planillas(any(), any());
    }

    @Test
    void descargar_entregaUnaDireccionTemporalFirmada_sinLaRuta() {
        when(almacenamiento.existe(RUTA)).thenReturn(true);
        when(almacenamiento.urlTemporal(RUTA, AlmacenamientoService.VIGENCIA_ACCESO))
                .thenReturn("https://minio.local/firmada");

        ResultadoAuditado<AccesoPlanillaResponse> resultado = service.descargar(actorCodigo, planillaCodigo);

        assertThat(resultado.datos().url()).isEqualTo("https://minio.local/firmada");
        assertThat(resultado.datos().expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(5)));
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.actorId()).isEqualTo(1L);
        assertThat(resultado.entidadId()).isEqualTo(60L);
        assertThat(resultado.detalle()).containsEntry("planilla", planillaCodigo.toString());
        assertThat(resultado.detalle().toString()).doesNotContain("planillas/", "minio");
    }

    @Test
    void descargar_planillaInexistente_lanzaErrPla05() {
        when(planillaRepository.findByCodigo(planillaCodigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descargar(actorCodigo, planillaCodigo))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PLA_05"));
    }

    @Test
    void descargar_sinArchivoEnElAlmacenamiento_lanzaErrPla06_yNoFirmaNada() {
        when(almacenamiento.existe(RUTA)).thenReturn(false);

        assertThatThrownBy(() -> service.descargar(actorCodigo, planillaCodigo))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PLA_06"));

        verify(almacenamiento, never()).urlTemporal(any(), any());
    }

    @Test
    void losMetodosExigenElRolAdministradorDubss_yDescargarSeAnotaEnLaBitacora() {
        for (var metodo : ConsultaPlanillasService.class.getDeclaredMethods()) {
            if (metodo.getName().equals("listar") || metodo.getName().equals("descargar")) {
                assertThat(metodo.getAnnotation(PreAuthorize.class).value())
                        .isEqualTo("hasRole('ADMINISTRADOR_DUBSS')");
            }
            if (metodo.getName().equals("descargar")) {
                assertThat(metodo.getAnnotation(Auditar.class).accion())
                        .isEqualTo(BitacoraAcciones.DESCARGA_PLANILLA);
            }
        }
    }
}
