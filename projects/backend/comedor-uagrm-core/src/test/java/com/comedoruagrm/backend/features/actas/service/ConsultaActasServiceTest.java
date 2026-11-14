package com.comedoruagrm.backend.features.actas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;
import com.comedoruagrm.backend.features.actas.dto.AccesoActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaActaResponse;
import com.comedoruagrm.backend.features.actas.dto.ConvocatoriaPendienteActaResponse;
import com.comedoruagrm.backend.features.actas.model.ActaAdjudicacion;
import com.comedoruagrm.backend.features.actas.repository.ActaAdjudicacionRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository;
import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Listar, consultar y descargar actas (CU08) con los colaboradores simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultaActasServiceTest {

    private static final long CONVOCATORIA_ID = 7L;
    private static final Instant AHORA = Instant.parse("2026-11-13T15:30:00Z");
    private static final String RUTA = "actas/secreta/acta-001-2026.pdf";
    private static final String URL_FIRMADA = "http://minio.local/firmada?X-Amz-Signature=abc";

    @Mock
    private ActasConsultaRepository consulta;

    @Mock
    private ActaAdjudicacionRepository actaRepository;

    @Mock
    private AlmacenamientoService almacenamiento;

    @Mock
    private UsuarioService usuarioService;

    private ConsultaActasService service;
    private Usuario admin;
    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID convocatoria = UUID.randomUUID();
    private final UUID actaCodigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new ConsultaActasService(consulta, actaRepository, almacenamiento, usuarioService,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        admin = new Usuario(null, "0000001", "admin@dubss.com", "hash", "Admin", "DUBSS");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "codigo", actorCodigo);
        when(usuarioService.obtenerPorCodigo(actorCodigo)).thenReturn(admin);
    }

    private void convocatoriaExistente() {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.of(new DatosConvocatoria(CONVOCATORIA_ID,
                convocatoria, "Beca 2026-II", "PUBLICADA", "2026-II", AHORA, AHORA.plusSeconds(60), 50,
                new BigDecimal("60.00"))));
    }

    private ActaAdjudicacion actaRegistrada() {
        ActaAdjudicacion acta = new ActaAdjudicacion(CONVOCATORIA_ID, "001/2026", RUTA, 1L, AHORA);
        ReflectionTestUtils.setField(acta, "id", 50L);
        ReflectionTestUtils.setField(acta, "codigo", actaCodigo);
        return acta;
    }

    private ActaResponse respuesta() {
        return new ActaResponse(actaCodigo, "001/2026", new ConvocatoriaActaResponse(convocatoria, "Beca 2026-II"),
                "2026-II", 3, AHORA, "Admin DUBSS");
    }

    // ---- listar y pendientes ----

    @Test
    void listar_devuelveLaPaginaDelRepository_conLaBusquedaYElOrdenRecibidos() {
        Pageable pageable = PageRequest.of(1, 10);
        Page<ActaResponse> pagina = new PageImpl<>(List.of(respuesta()), pageable, 11);
        when(consulta.actas("001", pageable)).thenReturn(pagina);

        assertThat(service.listar("001", pageable)).isSameAs(pagina);
    }

    @Test
    void listar_conUnSortNoSoportado_laExcepcionDelRepositorySube() {
        Pageable pageable = PageRequest.of(0, 20);
        when(consulta.actas(null, pageable)).thenThrow(new ValidationException("ERR_SYS_05", "sort no soportado"));

        assertThatThrownBy(() -> service.listar(null, pageable)).isInstanceOfSatisfying(ValidationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_05"));
    }

    @Test
    void pendientes_devuelveSoloLoQueElRepositoryTraeSinActa() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ConvocatoriaPendienteActaResponse> pagina = new PageImpl<>(
                List.of(new ConvocatoriaPendienteActaResponse(convocatoria, "Beca 2026-II", AHORA, 3)), pageable, 1);
        when(consulta.convocatoriasPendientes(pageable)).thenReturn(pagina);

        assertThat(service.pendientes(pageable).getContent()).hasSize(1);
        verify(consulta, times(1)).convocatoriasPendientes(pageable);
    }

    // ---- obtener ----

    @Test
    void obtener_conActa_laDevuelveSinAnotarNada() {
        convocatoriaExistente();
        when(consulta.acta(CONVOCATORIA_ID)).thenReturn(Optional.of(respuesta()));

        ActaResponse acta = service.obtener(convocatoria);

        assertThat(acta.numero()).isEqualTo("001/2026");
        assertThat(acta.toString()).doesNotContain("actas/");
    }

    @Test
    void obtener_sinActa_lanzaErrAct03() {
        convocatoriaExistente();
        when(consulta.acta(CONVOCATORIA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(convocatoria)).isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_03"));
    }

    @Test
    void obtener_convocatoriaInexistente_lanzaErrPri01() {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(convocatoria)).isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PRI_01"));
        verify(consulta, never()).acta(org.mockito.ArgumentMatchers.anyLong());
    }

    // ---- descargar ----

    @Test
    void descargar_conActaYArchivo_entregaLaDireccionTemporalConSuVencimiento() {
        convocatoriaExistente();
        when(actaRepository.findByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(Optional.of(actaRegistrada()));
        when(almacenamiento.existe(RUTA)).thenReturn(true);
        when(almacenamiento.urlTemporal(RUTA, AlmacenamientoService.VIGENCIA_ACCESO)).thenReturn(URL_FIRMADA);

        ResultadoAuditado<AccesoActaResponse> resultado = service.descargar(actorCodigo, convocatoria);

        assertThat(resultado.datos().url()).isEqualTo(URL_FIRMADA);
        assertThat(resultado.datos().expiraEn()).isEqualTo(AHORA.plus(AlmacenamientoService.VIGENCIA_ACCESO));
        assertThat(AlmacenamientoService.VIGENCIA_ACCESO.toMinutes()).isEqualTo(5);
        assertThat(resultado.actorId()).isEqualTo(1L);
        assertThat(resultado.entidadId()).isEqualTo(50L);
        assertThat(resultado.auditar()).isTrue();
    }

    @Test
    void descargar_laBitacoraLlevaSoloLosCodigosDeActaYConvocatoria() {
        convocatoriaExistente();
        when(actaRepository.findByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(Optional.of(actaRegistrada()));
        when(almacenamiento.existe(RUTA)).thenReturn(true);
        when(almacenamiento.urlTemporal(anyString(), any())).thenReturn(URL_FIRMADA);

        var detalle = service.descargar(actorCodigo, convocatoria).detalle();

        assertThat(detalle.keySet()).containsExactly("acta", "convocatoria");
        assertThat(detalle).containsEntry("acta", actaCodigo.toString())
                .containsEntry("convocatoria", convocatoria.toString());
        assertThat(detalle.toString()).doesNotContain("actas/", "http", "Signature", "Admin", "@");
    }

    @Test
    void descargar_sinActa_lanzaErrAct03_yNoFirmaNada() {
        convocatoriaExistente();
        when(actaRepository.findByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descargar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_03"));

        verify(almacenamiento, never()).urlTemporal(anyString(), any());
    }

    @Test
    void descargar_conArchivoAusente_lanzaErrAct04_yNoFirmaNada() {
        convocatoriaExistente();
        when(actaRepository.findByConvocatoriaId(CONVOCATORIA_ID)).thenReturn(Optional.of(actaRegistrada()));
        when(almacenamiento.existe(RUTA)).thenReturn(false);

        assertThatThrownBy(() -> service.descargar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(NotFoundException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo("ERR_ACT_04");
                    assertThat(e.getMessage()).doesNotContain("actas/");
                });

        verify(almacenamiento, never()).urlTemporal(anyString(), any());
    }

    @Test
    void descargar_convocatoriaInexistente_lanzaErrPri01() {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.descargar(actorCodigo, convocatoria))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PRI_01"));

        verify(actaRepository, never()).findByConvocatoriaId(org.mockito.ArgumentMatchers.anyLong());
    }

    // ---- permisos y bitacora ----

    @Test
    void todosLosMetodosExigenElRolAdministrador() throws NoSuchMethodException {
        var metodos = List.of(
                ConsultaActasService.class.getMethod("listar", String.class, Pageable.class),
                ConsultaActasService.class.getMethod("pendientes", Pageable.class),
                ConsultaActasService.class.getMethod("obtener", UUID.class),
                ConsultaActasService.class.getMethod("descargar", UUID.class, UUID.class));

        for (var metodo : metodos) {
            assertThat(metodo.getAnnotation(PreAuthorize.class).value())
                    .as(metodo.getName()).isEqualTo("hasRole('ADMINISTRADOR_DUBSS')");
        }
    }

    @Test
    void soloDescargarSeAnotaEnLaBitacora_yLasLecturasSonDeSoloLectura() throws NoSuchMethodException {
        var descargar = ConsultaActasService.class.getMethod("descargar", UUID.class, UUID.class);

        assertThat(descargar.getAnnotation(Auditar.class).accion()).isEqualTo(BitacoraAcciones.DESCARGA_ACTA);
        assertThat(descargar.getAnnotation(Auditar.class).entidad()).isEqualTo("acta_adjudicacion");
        assertThat(descargar.getAnnotation(Transactional.class).readOnly()).isFalse();
        for (var lectura : List.of(
                ConsultaActasService.class.getMethod("listar", String.class, Pageable.class),
                ConsultaActasService.class.getMethod("pendientes", Pageable.class),
                ConsultaActasService.class.getMethod("obtener", UUID.class))) {
            assertThat(lectura.getAnnotation(Auditar.class)).as(lectura.getName()).isNull();
            assertThat(lectura.getAnnotation(Transactional.class).readOnly()).isTrue();
        }
    }
}
