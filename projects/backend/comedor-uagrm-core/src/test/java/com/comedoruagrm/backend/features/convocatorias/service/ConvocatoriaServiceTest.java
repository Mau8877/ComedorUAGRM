package com.comedoruagrm.backend.features.convocatorias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.features.convocatorias.ConvocatoriaPruebas;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaRequest;
import com.comedoruagrm.backend.features.convocatorias.dto.ConvocatoriaResponse;
import com.comedoruagrm.backend.features.convocatorias.dto.DocumentoRequeridoRequest;
import com.comedoruagrm.backend.features.convocatorias.enums.EstadoConvocatoria;
import com.comedoruagrm.backend.features.convocatorias.exception.ConvocatoriaErrorCodes;
import com.comedoruagrm.backend.features.convocatorias.mapper.ConvocatoriaMapper;
import com.comedoruagrm.backend.features.convocatorias.model.Convocatoria;
import com.comedoruagrm.backend.features.convocatorias.model.GestionAcademica;
import com.comedoruagrm.backend.features.convocatorias.model.TipoDocumento;
import com.comedoruagrm.backend.features.convocatorias.repository.ConvocatoriaRepository;
import com.comedoruagrm.backend.features.convocatorias.repository.GestionAcademicaRepository;
import com.comedoruagrm.backend.features.convocatorias.service.ConvocatoriaService.PaginaConvocatorias;
import com.comedoruagrm.backend.features.postulaciones.service.PostulacionesDeConvocatoriaService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;

class ConvocatoriaServiceTest {

    private final ConvocatoriaRepository convocatoriaRepository = mock(ConvocatoriaRepository.class);
    private final GestionAcademicaRepository gestionRepository = mock(GestionAcademicaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final DocumentosConvocatoriaService documentos = mock(DocumentosConvocatoriaService.class);
    private final PostulacionesDeConvocatoriaService postulaciones = mock(PostulacionesDeConvocatoriaService.class);
    private final ConvocatoriaMapper mapper = mock(ConvocatoriaMapper.class);
    private final ConvocatoriaService service = new ConvocatoriaService(convocatoriaRepository, gestionRepository,
            usuarioRepository, documentos, postulaciones, mapper);

    private final UUID adminCodigo = UUID.randomUUID();
    private final Usuario admin = mock(Usuario.class);
    private final GestionAcademica gestion = ConvocatoriaPruebas.gestion(1L, "2026-II");
    private final TipoDocumento carnet = ConvocatoriaPruebas.tipo(10L, "Carnet");
    private final Map<TipoDocumento, Boolean> requeridos = Map.of(carnet, true);

    @BeforeEach
    void preparar() {
        when(admin.getId()).thenReturn(7L);
        when(admin.getCodigo()).thenReturn(adminCodigo);
        when(usuarioRepository.findByCodigo(adminCodigo)).thenReturn(Optional.of(admin));
        when(gestionRepository.findByCodigo(gestion.getCodigo())).thenReturn(Optional.of(gestion));
        when(documentos.validar(any())).thenReturn(requeridos);
        when(convocatoriaRepository.saveAndFlush(any(Convocatoria.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.detalle(any())).thenAnswer(inv -> ConvocatoriaPruebas.detalle(inv.getArgument(0)));
    }

    private ConvocatoriaRequest request(LocalDate apertura, LocalDate cierre) {
        return new ConvocatoriaRequest("Beca Comedor 2026-II", gestion.getCodigo(), apertura, cierre, 100,
                new BigDecimal("40.00"), 120, List.of(new DocumentoRequeridoRequest(carnet.getCodigo(), true)));
    }

    private ConvocatoriaRequest requestValida() {
        return request(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
    }

    private Convocatoria existente(EstadoConvocatoria estado) {
        Convocatoria convocatoria = ConvocatoriaPruebas.convocatoria(5L, gestion, estado);
        when(convocatoriaRepository.findByCodigo(convocatoria.getCodigo())).thenReturn(Optional.of(convocatoria));
        return convocatoria;
    }

    private void esperarError(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(codigo));
    }

    // ================= listar y obtener =================

    @Test
    void listar_mapeaLaPaginaYPasaLosFiltrosAlRepositorio() {
        Convocatoria convocatoria = ConvocatoriaPruebas.convocatoria(5L, gestion, EstadoConvocatoria.ABIERTA);
        when(convocatoriaRepository.buscar(any(), any(), any(), any())).thenReturn(
                new PageImpl<>(List.of(convocatoria), PageRequest.of(0, 20), 41));

        PaginaConvocatorias pagina = service.listar(1, 20, " Beca ", EstadoConvocatoria.ABIERTA,
                gestion.getCodigo(), null);

        assertThat(pagina.datos()).hasSize(1);
        assertThat(pagina.datos().get(0).estado()).isEqualTo(EstadoConvocatoria.ABIERTA);
        assertThat(pagina.meta().totalItems()).isEqualTo(41);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(convocatoriaRepository).buscar(eq("%beca%"), eq(EstadoConvocatoria.ABIERTA), eq(gestion.getCodigo()),
                pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("creadoEn").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listar_conSortNoSoportado_lanzaErrCv11() {
        esperarError(() -> service.listar(1, 20, null, null, null, "cupos"), ConvocatoriaErrorCodes.ERR_CV_11);
    }

    @Test
    void obtener_devuelveElDetalle() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.BORRADOR);

        assertThat(service.obtener(convocatoria.getCodigo()).codigo()).isEqualTo(convocatoria.getCodigo());
    }

    @Test
    void obtener_inexistente_lanzaErrCv01() {
        esperarError(() -> service.obtener(UUID.randomUUID()), ConvocatoriaErrorCodes.ERR_CV_01);
    }

    // ================= crear =================

    @Test
    void crear_guardaEnBorradorConElActorYSusDocumentos() {
        ResultadoAuditado<ConvocatoriaResponse> resultado = service.crear(adminCodigo, requestValida());

        ArgumentCaptor<Convocatoria> guardada = ArgumentCaptor.forClass(Convocatoria.class);
        verify(convocatoriaRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoConvocatoria.BORRADOR);
        assertThat(guardada.getValue().getCreadoPor()).isEqualTo(7L);
        assertThat(guardada.getValue().getFechaApertura())
                .isEqualTo(FechasConvocatoria.aperturaDesde(LocalDate.of(2026, 8, 1)));
        assertThat(guardada.getValue().getFechaCierre())
                .isEqualTo(FechasConvocatoria.cierreHasta(LocalDate.of(2026, 8, 31)));
        verify(documentos).reemplazar(guardada.getValue(), requeridos);
        assertThat(resultado.actorId()).isEqualTo(7L);
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.detalle()).containsEntry("cantidadDocumentos", 1)
                .containsEntry("gestion", gestion.getCodigo().toString());
    }

    @Test
    void crear_conOpcionalesNulos_seGuardaIgual() {
        ConvocatoriaRequest base = requestValida();

        service.crear(adminCodigo, new ConvocatoriaRequest(base.nombre(), base.gestionCodigo(),
                base.fechaApertura(), base.fechaCierre(), null, null, null, base.documentos()));

        ArgumentCaptor<Convocatoria> guardada = ArgumentCaptor.forClass(Convocatoria.class);
        verify(convocatoriaRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getCupos()).isNull();
        assertThat(guardada.getValue().getPuntajeMinimo()).isNull();
        assertThat(guardada.getValue().getCantidadFinalistas()).isNull();
    }

    @Test
    void crear_conCierreIgualOAnteriorALaApertura_lanzaErrCv04SinGuardar() {
        LocalDate dia = LocalDate.of(2026, 8, 1);

        esperarError(() -> service.crear(adminCodigo, request(dia, dia)), ConvocatoriaErrorCodes.ERR_CV_04);
        esperarError(() -> service.crear(adminCodigo, request(dia, dia.minusDays(1))),
                ConvocatoriaErrorCodes.ERR_CV_04);
        verify(convocatoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_conGestionInexistente_lanzaErrCv02() {
        ConvocatoriaRequest base = requestValida();

        esperarError(() -> service.crear(adminCodigo, new ConvocatoriaRequest(base.nombre(), UUID.randomUUID(),
                base.fechaApertura(), base.fechaCierre(), 1, null, 1, base.documentos())),
                ConvocatoriaErrorCodes.ERR_CV_02);
    }

    @Test
    void crear_siLosDocumentosSonInvalidos_noGuardaNada() {
        when(documentos.validar(any())).thenThrow(new com.comedoruagrm.backend.common.exception.ValidationException(
                ConvocatoriaErrorCodes.ERR_CV_05, "x"));

        esperarError(() -> service.crear(adminCodigo, requestValida()), ConvocatoriaErrorCodes.ERR_CV_05);
        verify(convocatoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_elDetalleDeLaBitacoraNoLlevaElNombre() {
        ResultadoAuditado<ConvocatoriaResponse> resultado = service.crear(adminCodigo, requestValida());

        assertThat(resultado.detalle().values()).doesNotContain("Beca Comedor 2026-II");
    }

    // ================= editar =================

    @Test
    void editar_cambiaSoloLoQueDifiere() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.ABIERTA);
        when(documentos.difiere(convocatoria, requeridos)).thenReturn(false);
        ConvocatoriaRequest cambio = new ConvocatoriaRequest(convocatoria.getNombre(), gestion.getCodigo(),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 15), 100, new BigDecimal("40"), 120,
                List.of(new DocumentoRequeridoRequest(carnet.getCodigo(), true)));

        ResultadoAuditado<ConvocatoriaResponse> resultado =
                service.editar(adminCodigo, convocatoria.getCodigo(), cambio);

        assertThat(convocatoria.getFechaCierre()).isEqualTo(FechasConvocatoria.cierreHasta(LocalDate.of(2026, 9, 15)));
        assertThat(resultado.detalle()).containsEntry("camposModificados", List.of("fechaCierre"));
        verify(documentos, never()).reemplazar(any(), any());
    }

    @Test
    void editar_conOpcionalesNulos_losVacia() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.BORRADOR);
        ConvocatoriaRequest base = requestValida();

        ResultadoAuditado<ConvocatoriaResponse> resultado = service.editar(adminCodigo, convocatoria.getCodigo(),
                new ConvocatoriaRequest(convocatoria.getNombre(), gestion.getCodigo(), base.fechaApertura(),
                        base.fechaCierre(), null, null, null, base.documentos()));

        assertThat(convocatoria.getCupos()).isNull();
        assertThat(convocatoria.getPuntajeMinimo()).isNull();
        assertThat(convocatoria.getCantidadFinalistas()).isNull();
        assertThat(resultado.detalle().get("camposModificados"))
                .isEqualTo(List.of("cupos", "puntajeMinimo", "cantidadFinalistas"));
    }

    @Test
    void editar_conElMismoPuntajeConOtraEscala_noCuentaComoCambio() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.BORRADOR);
        ConvocatoriaRequest base = requestValida();

        ResultadoAuditado<ConvocatoriaResponse> resultado = service.editar(adminCodigo, convocatoria.getCodigo(),
                new ConvocatoriaRequest(convocatoria.getNombre(), gestion.getCodigo(), base.fechaApertura(),
                        base.fechaCierre(), 100, new BigDecimal("40"), 120, base.documentos()));

        assertThat(resultado.auditar()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoConvocatoria.class, names = {"CERRADA", "PUBLICADA"})
    void editar_unaCerradaOPublicada_lanzaErrCv08(EstadoConvocatoria estado) {
        Convocatoria convocatoria = existente(estado);

        esperarError(() -> service.editar(adminCodigo, convocatoria.getCodigo(), requestValida()),
                ConvocatoriaErrorCodes.ERR_CV_08);
    }

    @Test
    void editar_cambiarLaGestionEnUnaAbierta_lanzaErrCv08() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.ABIERTA);
        GestionAcademica otra = ConvocatoriaPruebas.gestion(2L, "2027-I");
        when(gestionRepository.findByCodigo(otra.getCodigo())).thenReturn(Optional.of(otra));
        ConvocatoriaRequest base = requestValida();

        esperarError(() -> service.editar(adminCodigo, convocatoria.getCodigo(), new ConvocatoriaRequest(
                base.nombre(), otra.getCodigo(), base.fechaApertura(), base.fechaCierre(), 1, null, 1,
                base.documentos())), ConvocatoriaErrorCodes.ERR_CV_08);
    }

    @Test
    void editar_cambiarLaGestionEnBorrador_esValido() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.BORRADOR);
        GestionAcademica otra = ConvocatoriaPruebas.gestion(2L, "2027-I");
        when(gestionRepository.findByCodigo(otra.getCodigo())).thenReturn(Optional.of(otra));
        ConvocatoriaRequest base = requestValida();

        ResultadoAuditado<ConvocatoriaResponse> resultado = service.editar(adminCodigo, convocatoria.getCodigo(),
                new ConvocatoriaRequest(convocatoria.getNombre(), otra.getCodigo(), base.fechaApertura(),
                        base.fechaCierre(), 100, new BigDecimal("40.00"), 120, base.documentos()));

        assertThat(convocatoria.getGestion()).isSameAs(otra);
        assertThat(resultado.detalle().get("camposModificados")).isEqualTo(List.of("gestion"));
    }

    @Test
    void editar_conPostulaciones_noPermiteCambiarLaFechaDeAperturaNiLosDocumentos() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.ABIERTA);
        when(postulaciones.existenPara(5L)).thenReturn(true);
        ConvocatoriaRequest base = requestValida();

        // Otra fecha de apertura.
        esperarError(() -> service.editar(adminCodigo, convocatoria.getCodigo(), new ConvocatoriaRequest(
                base.nombre(), gestion.getCodigo(), LocalDate.of(2026, 7, 20), base.fechaCierre(), 100,
                new BigDecimal("40.00"), 120, base.documentos())), ConvocatoriaErrorCodes.ERR_CV_09);

        // Otros documentos.
        when(documentos.difiere(convocatoria, requeridos)).thenReturn(true);
        esperarError(() -> service.editar(adminCodigo, convocatoria.getCodigo(), base),
                ConvocatoriaErrorCodes.ERR_CV_09);
        verify(convocatoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void editar_conPostulaciones_sePuedeCambiarElNombreElCierreYLosCupos() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.ABIERTA);
        when(postulaciones.existenPara(5L)).thenReturn(true);
        when(documentos.difiere(convocatoria, requeridos)).thenReturn(false);

        ResultadoAuditado<ConvocatoriaResponse> resultado = service.editar(adminCodigo, convocatoria.getCodigo(),
                new ConvocatoriaRequest("Otro nombre", gestion.getCodigo(), LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 9, 30), 150, new BigDecimal("40.00"), 120,
                        List.of(new DocumentoRequeridoRequest(carnet.getCodigo(), true))));

        assertThat(resultado.detalle().get("camposModificados"))
                .isEqualTo(List.of("nombre", "fechaCierre", "cupos"));
    }

    @Test
    void editar_reemplazaLosDocumentosCuandoCambian() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.BORRADOR);
        when(documentos.difiere(convocatoria, requeridos)).thenReturn(true);

        ResultadoAuditado<ConvocatoriaResponse> resultado =
                service.editar(adminCodigo, convocatoria.getCodigo(), requestValida());

        verify(documentos).reemplazar(convocatoria, requeridos);
        assertThat(resultado.detalle().get("camposModificados")).isEqualTo(List.of("documentos"));
    }

    @Test
    void editar_sinCambios_noSeAnotaNiSeGuarda() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.BORRADOR);
        when(documentos.difiere(convocatoria, requeridos)).thenReturn(false);

        ResultadoAuditado<ConvocatoriaResponse> resultado = service.editar(adminCodigo, convocatoria.getCodigo(),
                new ConvocatoriaRequest(convocatoria.getNombre(), gestion.getCodigo(), LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31), 100, new BigDecimal("40.00"), 120,
                        List.of(new DocumentoRequeridoRequest(carnet.getCodigo(), true))));

        assertThat(resultado.auditar()).isFalse();
        verify(convocatoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void editar_inexistente_lanzaErrCv01() {
        esperarError(() -> service.editar(adminCodigo, UUID.randomUUID(), requestValida()),
                ConvocatoriaErrorCodes.ERR_CV_01);
    }

    @Test
    void editar_conFechasIncoherentes_lanzaErrCv04() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.BORRADOR);
        LocalDate dia = LocalDate.of(2026, 8, 1);

        esperarError(() -> service.editar(adminCodigo, convocatoria.getCodigo(), request(dia, dia)),
                ConvocatoriaErrorCodes.ERR_CV_04);
    }

    // ================= eliminar =================

    @Test
    void eliminar_unaBorradorSinPostulaciones_laEliminaConSusDocumentos() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.BORRADOR);

        ResultadoAuditado<Void> resultado = service.eliminar(adminCodigo, convocatoria.getCodigo());

        assertThat(convocatoria.getEliminadoEn()).isNotNull();
        verify(documentos).eliminarTodos(convocatoria);
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.entidadId()).isEqualTo(5L);
    }

    @Test
    void eliminar_conPostulaciones_lanzaErrCv10AunqueEsteEnBorrador() {
        Convocatoria convocatoria = existente(EstadoConvocatoria.BORRADOR);
        when(postulaciones.existenPara(5L)).thenReturn(true);

        esperarError(() -> service.eliminar(adminCodigo, convocatoria.getCodigo()),
                ConvocatoriaErrorCodes.ERR_CV_10);
        verify(documentos, never()).eliminarTodos(any());
    }

    @ParameterizedTest
    @EnumSource(value = EstadoConvocatoria.class, names = {"ABIERTA", "CERRADA", "PUBLICADA"})
    void eliminar_siNoEstaEnBorrador_lanzaErrCv08(EstadoConvocatoria estado) {
        Convocatoria convocatoria = existente(estado);

        esperarError(() -> service.eliminar(adminCodigo, convocatoria.getCodigo()),
                ConvocatoriaErrorCodes.ERR_CV_08);
        assertThat(convocatoria.getEliminadoEn()).isNull();
    }

    @Test
    void eliminar_inexistente_lanzaErrCv01() {
        esperarError(() -> service.eliminar(adminCodigo, UUID.randomUUID()), ConvocatoriaErrorCodes.ERR_CV_01);
    }
}
