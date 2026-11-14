package com.comedoruagrm.backend.features.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ForbiddenException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.auth.service.SesionService;
import com.comedoruagrm.backend.features.usuarios.constant.RolNombres;
import com.comedoruagrm.backend.features.usuarios.dto.ActualizarUsuarioPersonalRequest;
import com.comedoruagrm.backend.features.usuarios.dto.CambiarRolRequest;
import com.comedoruagrm.backend.features.usuarios.dto.CrearUsuarioPersonalRequest;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioCreadoResponse;
import com.comedoruagrm.backend.features.usuarios.dto.UsuarioPersonalResponse;
import com.comedoruagrm.backend.features.usuarios.exception.UsuarioErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.repository.RolRepository;
import com.comedoruagrm.backend.features.usuarios.repository.UsuarioRepository;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioPersonalService.PaginaUsuariosPersonal;

class UsuarioPersonalServiceTest {

    private static final String TEMPORAL = "Abc23456xyz9";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final RolRepository rolRepository = mock(RolRepository.class);
    private final SesionService sesionService = mock(SesionService.class);
    private final GeneradorContrasenaTemporal generador = mock(GeneradorContrasenaTemporal.class);
    // Costo 4 (el minimo): los tests no necesitan un hash lento.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final UsuarioPersonalService service = new UsuarioPersonalService(
            usuarioRepository, rolRepository, sesionService, passwordEncoder, generador);

    private final UUID adminCodigo = UUID.randomUUID();
    private final Usuario admin = cuenta(1L, RolNombres.ADMINISTRADOR_DUBSS, true, adminCodigo);

    @BeforeEach
    void preparar() {
        when(usuarioRepository.findByCodigo(adminCodigo)).thenReturn(Optional.of(admin));
        when(generador.generar()).thenReturn(TEMPORAL);
        for (String nombre : List.of(RolNombres.ADMINISTRADOR_DUBSS, RolNombres.DIRECTIVO,
                RolNombres.PERSONAL_COMEDOR)) {
            Rol rol = mock(Rol.class);
            when(rol.getNombre()).thenReturn(nombre);
            when(rolRepository.findByNombre(nombre)).thenReturn(Optional.of(rol));
        }
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ================= listar (UT-CU14-BE-07) =================

    @Test
    void listar_mapeaLaPaginaYSuMeta() {
        Usuario directivo = cuenta(5L, RolNombres.DIRECTIVO, true, UUID.randomUUID());
        when(directivo.getNombres()).thenReturn("Dario");
        when(directivo.getApellidos()).thenReturn("Rojas");
        when(directivo.getCi()).thenReturn("123");
        when(directivo.getCorreo()).thenReturn("dario@uagrm.edu.bo");
        when(usuarioRepository.buscarPersonal(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(directivo), PageRequest.of(0, 20), 41));

        PaginaUsuariosPersonal pagina = service.listar(1, 20, null, null, null, null);

        assertThat(pagina.meta().page()).isEqualTo(1);
        assertThat(pagina.meta().pageSize()).isEqualTo(20);
        assertThat(pagina.meta().totalItems()).isEqualTo(41);
        assertThat(pagina.meta().totalPages()).isEqualTo(3);
        UsuarioPersonalResponse fila = pagina.datos().get(0);
        assertThat(fila.codigo()).isEqualTo(directivo.getCodigo());
        assertThat(fila.rol()).isEqualTo("DIRECTIVO");
        assertThat(fila.activo()).isTrue();
        assertThat(fila.ci()).isEqualTo("123");
    }

    @Test
    void listar_pasaLosFiltrosYLaBusquedaEnMinusculasComoPatron() {
        when(usuarioRepository.buscarPersonal(any(), any(), any(), any())).thenReturn(Page.empty());

        service.listar(2, 10, "  María ", RolNombres.DIRECTIVO, false, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioRepository).buscarPersonal(org.mockito.ArgumentMatchers.eq("%maría%"),
                org.mockito.ArgumentMatchers.eq(RolNombres.DIRECTIVO), org.mockito.ArgumentMatchers.eq(false),
                pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void listar_sinSort_ordenaPorCreadoEnDescendente() {
        when(usuarioRepository.buscarPersonal(any(), any(), any(), any())).thenReturn(Page.empty());

        service.listar(1, 20, null, null, null, null);

        Sort orden = pageableEnviado().getSort();
        assertThat(orden.getOrderFor("creadoEn")).isNotNull();
        assertThat(orden.getOrderFor("creadoEn").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listar_conSortPorRol_ordenaPorElNombreDelRol() {
        when(usuarioRepository.buscarPersonal(any(), any(), any(), any())).thenReturn(Page.empty());

        service.listar(1, 20, null, null, null, "rol");

        assertThat(pageableEnviado().getSort().getOrderFor("rol.nombre").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void listar_conSortInvalido_lanzaErrUs11() {
        assertThatThrownBy(() -> service.listar(1, 20, null, null, null, "passwordHash"))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_11));
        verifyNoInteractions(usuarioRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {RolNombres.POSTULANTE, "ROL_QUE_NO_EXISTE"})
    void listar_conUnRolQueNoEsDelPersonal_lanzaErrUs10(String rol) {
        assertThatThrownBy(() -> service.listar(1, 20, null, rol, null, null))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_10));
    }

    @Test
    void listar_conPaginaOTamanoInvalidos_lanzaErrSys01() {
        assertThatThrownBy(() -> service.listar(0, 20, null, null, null, null))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_SYS_01"));
        assertThatThrownBy(() -> service.listar(1, 0, null, null, null, null))
                .isInstanceOf(ValidationException.class);
    }

    // ================= crear (UT-CU14-BE-08) =================

    @ParameterizedTest
    @ValueSource(strings = {RolNombres.ADMINISTRADOR_DUBSS, RolNombres.DIRECTIVO, RolNombres.PERSONAL_COMEDOR})
    void crear_conCadaRolDelPersonal_creaLaCuentaActivaYDevuelveLaContrasenaTemporal(String rol) {
        ResultadoAuditado<UsuarioCreadoResponse> resultado = service.crear(adminCodigo, nuevaCuenta(rol));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getRol().getNombre()).isEqualTo(rol);
        assertThat(guardado.getValue().isActivo()).isTrue();
        assertThat(guardado.getValue().getCorreo()).isEqualTo("ana@uagrm.edu.bo");
        assertThat(resultado.datos().contrasenaTemporal()).isEqualTo(TEMPORAL);
        assertThat(resultado.datos().rol()).isEqualTo(rol);
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.detalle()).containsEntry("rol", rol);
    }

    @Test
    void crear_guardaSoloElHashDeLaContrasenaTemporal() {
        service.crear(adminCodigo, nuevaCuenta(RolNombres.DIRECTIVO));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getPasswordHash()).isNotEqualTo(TEMPORAL).startsWith("$2");
        assertThat(passwordEncoder.matches(TEMPORAL, guardado.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void crear_anotaElAdministradorComoActorYLaCuentaCreadaComoEntidad() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> {
            Usuario nuevo = inv.getArgument(0);
            // Un Usuario real todavia no tiene id: se simula el que asigna la base.
            Usuario conId = mock(Usuario.class);
            Rol rol = nuevo.getRol();
            when(conId.getId()).thenReturn(77L);
            when(conId.getCodigo()).thenReturn(UUID.randomUUID());
            when(conId.getRol()).thenReturn(rol);
            when(conId.isActivo()).thenReturn(true);
            return conId;
        });

        ResultadoAuditado<UsuarioCreadoResponse> resultado = service.crear(adminCodigo,
                nuevaCuenta(RolNombres.DIRECTIVO));

        assertThat(resultado.actorId()).isEqualTo(1L);
        assertThat(resultado.actorCodigo()).isEqualTo(adminCodigo);
        assertThat(resultado.entidadId()).isEqualTo(77L);
    }

    @ParameterizedTest
    @ValueSource(strings = {RolNombres.POSTULANTE, "ROL_QUE_NO_EXISTE"})
    void crear_conUnRolQueNoEsDelPersonal_lanzaErrUs10SinCrearNiGenerarNada(String rol) {
        assertThatThrownBy(() -> service.crear(adminCodigo, nuevaCuenta(rol)))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_10));
        verify(usuarioRepository, never()).saveAndFlush(any());
        verifyNoInteractions(generador);
    }

    @Test
    void crear_conCiDuplicado_lanzaErrUs01SinCrearNiGenerarContrasena() {
        when(usuarioRepository.existsByCi("1234567")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(adminCodigo, nuevaCuenta(RolNombres.DIRECTIVO)))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_01));
        verify(usuarioRepository, never()).saveAndFlush(any());
        verifyNoInteractions(generador);
    }

    @Test
    void crear_conCorreoDuplicado_lanzaErrUs02SinCrearNiGenerarContrasena() {
        when(usuarioRepository.existsByCorreo("ana@uagrm.edu.bo")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(adminCodigo, nuevaCuenta(RolNombres.DIRECTIVO)))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_02));
        verifyNoInteractions(generador);
    }

    @Test
    void crear_normalizaElCorreoAMinusculasYSinEspacios() {
        service.crear(adminCodigo, new CrearUsuarioPersonalRequest(
                " Ana ", " Perez ", " 1234567 ", "  ANA@UAGRM.edu.bo ", RolNombres.DIRECTIVO));

        verify(usuarioRepository).existsByCorreo("ana@uagrm.edu.bo");
        verify(usuarioRepository).existsByCi("1234567");
    }

    // ================= editar (UT-CU14-BE-09) =================

    @Test
    void editar_cambiaSoloLosCamposQueLlegan() {
        Usuario cuenta = cuenta(5L, RolNombres.DIRECTIVO, true, UUID.randomUUID());
        when(cuenta.getNombres()).thenReturn("Ana");
        when(cuenta.getApellidos()).thenReturn("Perez");
        when(cuenta.getCorreo()).thenReturn("ana@uagrm.edu.bo");
        when(usuarioRepository.findPersonalByCodigo(cuenta.getCodigo())).thenReturn(Optional.of(cuenta));

        ResultadoAuditado<UsuarioPersonalResponse> resultado = service.editar(adminCodigo, cuenta.getCodigo(),
                new ActualizarUsuarioPersonalRequest(null, " Gomez ", null));

        verify(cuenta).setApellidos("Gomez");
        verify(cuenta, never()).setNombres(any());
        verify(cuenta, never()).setCorreo(any());
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.detalle()).containsEntry("camposModificados", List.of("apellidos"));
    }

    @Test
    void editar_conUnCorreoDeOtraCuenta_lanzaErrUs02() {
        Usuario cuenta = cuenta(5L, RolNombres.DIRECTIVO, true, UUID.randomUUID());
        when(cuenta.getCorreo()).thenReturn("ana@uagrm.edu.bo");
        when(usuarioRepository.findPersonalByCodigo(cuenta.getCodigo())).thenReturn(Optional.of(cuenta));
        Usuario ajena = cuenta(9L, RolNombres.DIRECTIVO, true, UUID.randomUUID());
        when(usuarioRepository.findByCorreo("otro@uagrm.edu.bo")).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.editar(adminCodigo, cuenta.getCodigo(),
                new ActualizarUsuarioPersonalRequest(null, null, "otro@uagrm.edu.bo")))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_02));
        verify(cuenta, never()).setCorreo(any());
    }

    @Test
    void editar_conSuPropioCorreoSinCambios_noEsConflictoNiSeAnota() {
        Usuario cuenta = cuenta(5L, RolNombres.DIRECTIVO, true, UUID.randomUUID());
        when(cuenta.getNombres()).thenReturn("Ana");
        when(cuenta.getCorreo()).thenReturn("ana@uagrm.edu.bo");
        when(usuarioRepository.findPersonalByCodigo(cuenta.getCodigo())).thenReturn(Optional.of(cuenta));

        ResultadoAuditado<UsuarioPersonalResponse> resultado = service.editar(adminCodigo, cuenta.getCodigo(),
                new ActualizarUsuarioPersonalRequest("Ana", null, " ANA@uagrm.edu.bo "));

        assertThat(resultado.auditar()).isFalse();
        verify(usuarioRepository, never()).findByCorreo(any());
        verify(usuarioRepository, never()).saveAndFlush(cuenta);
    }

    @Test
    void editar_conUnCorreoNuevoLibre_loCambia() {
        Usuario cuenta = cuenta(5L, RolNombres.DIRECTIVO, true, UUID.randomUUID());
        when(cuenta.getCorreo()).thenReturn("ana@uagrm.edu.bo");
        when(usuarioRepository.findPersonalByCodigo(cuenta.getCodigo())).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findByCorreo("nuevo@uagrm.edu.bo")).thenReturn(Optional.empty());

        service.editar(adminCodigo, cuenta.getCodigo(),
                new ActualizarUsuarioPersonalRequest(null, null, "Nuevo@uagrm.edu.bo"));

        verify(cuenta).setCorreo("nuevo@uagrm.edu.bo");
    }

    @Test
    void editar_conUnaCuentaInexistenteODePostulante_lanzaErrUs06() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.findPersonalByCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(adminCodigo, codigo,
                new ActualizarUsuarioPersonalRequest("Ana", null, null)))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_06));
    }

    @Test
    void editar_elDetalleDeLaBitacoraTieneSoloNombresDeCamposNoValores() {
        Usuario cuenta = cuenta(5L, RolNombres.DIRECTIVO, true, UUID.randomUUID());
        when(cuenta.getNombres()).thenReturn("Ana");
        when(cuenta.getCorreo()).thenReturn("ana@uagrm.edu.bo");
        when(usuarioRepository.findPersonalByCodigo(cuenta.getCodigo())).thenReturn(Optional.of(cuenta));

        var resultado = service.editar(adminCodigo, cuenta.getCodigo(),
                new ActualizarUsuarioPersonalRequest("Beatriz", null, "bea@uagrm.edu.bo"));

        assertThat(resultado.detalle().toString()).doesNotContain("Beatriz").doesNotContain("bea@uagrm");
        assertThat(resultado.detalle()).containsEntry("camposModificados", List.of("nombres", "correo"));
    }

    // ================= cambiarRol (UT-CU14-BE-10) =================

    @Test
    void cambiarRol_conUnCambioValido_guardaElRolCierraLasSesionesYAnotaAnteriorYNuevo() {
        Usuario cuenta = cuentaDelPersonal(5L, RolNombres.DIRECTIVO, true);

        ResultadoAuditado<UsuarioPersonalResponse> resultado = service.cambiarRol(adminCodigo, cuenta.getCodigo(),
                new CambiarRolRequest(RolNombres.PERSONAL_COMEDOR));

        verify(cuenta).setRol(any(Rol.class));
        verify(sesionService).revocarTodas(cuenta);
        assertThat(resultado.detalle()).containsEntry("rolAnterior", RolNombres.DIRECTIVO)
                .containsEntry("rolNuevo", RolNombres.PERSONAL_COMEDOR);
        assertThat(resultado.actorId()).isEqualTo(1L);
        assertThat(resultado.entidadId()).isEqualTo(5L);
    }

    @Test
    void cambiarRol_laPropiaCuenta_lanzaErrUs08AunqueSeaLaUltimaDeAdministrador() {
        when(usuarioRepository.findPersonalByCodigo(adminCodigo)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.cambiarRol(adminCodigo, adminCodigo,
                new CambiarRolRequest(RolNombres.DIRECTIVO)))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_08));
        verifyNoInteractions(sesionService);
    }

    @Test
    void cambiarRol_alMismoRol_lanzaErrUs12() {
        Usuario cuenta = cuentaDelPersonal(5L, RolNombres.DIRECTIVO, true);

        assertThatThrownBy(() -> service.cambiarRol(adminCodigo, cuenta.getCodigo(),
                new CambiarRolRequest(RolNombres.DIRECTIVO)))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_12));
        verifyNoInteractions(sesionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {RolNombres.POSTULANTE, "ROL_QUE_NO_EXISTE"})
    void cambiarRol_aUnRolQueNoEsDelPersonal_lanzaErrUs10(String rol) {
        Usuario cuenta = cuentaDelPersonal(5L, RolNombres.DIRECTIVO, true);

        assertThatThrownBy(() -> service.cambiarRol(adminCodigo, cuenta.getCodigo(), new CambiarRolRequest(rol)))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_10));
        verify(cuenta, never()).setRol(any());
    }

    @Test
    void cambiarRol_alUltimoAdministradorActivo_lanzaErrUs09SinCambiarNada() {
        Usuario otroAdmin = cuentaDelPersonal(5L, RolNombres.ADMINISTRADOR_DUBSS, true);
        when(usuarioRepository.bloquearAdministradoresActivos()).thenReturn(List.of(otroAdmin));

        assertThatThrownBy(() -> service.cambiarRol(adminCodigo, otroAdmin.getCodigo(),
                new CambiarRolRequest(RolNombres.DIRECTIVO)))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_09));
        verify(otroAdmin, never()).setRol(any());
        verifyNoInteractions(sesionService);
    }

    @Test
    void cambiarRol_aUnAdministradorActivoHabiendoOtro_esValido() {
        Usuario otroAdmin = cuentaDelPersonal(5L, RolNombres.ADMINISTRADOR_DUBSS, true);
        when(usuarioRepository.bloquearAdministradoresActivos()).thenReturn(List.of(otroAdmin, admin));

        service.cambiarRol(adminCodigo, otroAdmin.getCodigo(), new CambiarRolRequest(RolNombres.DIRECTIVO));

        verify(otroAdmin).setRol(any(Rol.class));
        verify(sesionService).revocarTodas(otroAdmin);
    }

    @Test
    void cambiarRol_aUnAdministradorYaDesactivado_noExigeOtroAdministradorActivo() {
        Usuario desactivado = cuentaDelPersonal(5L, RolNombres.ADMINISTRADOR_DUBSS, false);

        service.cambiarRol(adminCodigo, desactivado.getCodigo(), new CambiarRolRequest(RolNombres.DIRECTIVO));

        verify(usuarioRepository, never()).bloquearAdministradoresActivos();
        verify(desactivado).setRol(any(Rol.class));
    }

    @Test
    void cambiarRol_deUnaCuentaInexistenteODePostulante_lanzaErrUs06() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.findPersonalByCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarRol(adminCodigo, codigo, new CambiarRolRequest(RolNombres.DIRECTIVO)))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_06));
    }

    // ================= desactivar / reactivar (UT-CU14-BE-11) =================

    @Test
    void desactivar_unaCuentaNormal_laMarcaInactivaCierraLasSesionesYNoLaElimina() {
        Usuario cuenta = cuentaDelPersonal(5L, RolNombres.DIRECTIVO, true);

        ResultadoAuditado<UsuarioPersonalResponse> resultado = service.desactivar(adminCodigo, cuenta.getCodigo());

        verify(cuenta).setActivo(false);
        verify(cuenta, never()).setEliminadoEn(any());
        verify(sesionService).revocarTodas(cuenta);
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.actorId()).isEqualTo(1L);
        assertThat(resultado.entidadId()).isEqualTo(5L);
    }

    @Test
    void desactivar_laPropiaCuenta_lanzaErrUs08() {
        when(usuarioRepository.findPersonalByCodigo(adminCodigo)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.desactivar(adminCodigo, adminCodigo))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_08));
        verify(admin, never()).setActivo(false);
        verifyNoInteractions(sesionService);
    }

    @Test
    void desactivar_unaCuentaYaDesactivada_lanzaErrUs07() {
        Usuario cuenta = cuentaDelPersonal(5L, RolNombres.DIRECTIVO, false);

        assertThatThrownBy(() -> service.desactivar(adminCodigo, cuenta.getCodigo()))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_07));
        verifyNoInteractions(sesionService);
    }

    @Test
    void desactivar_alUltimoAdministradorActivo_lanzaErrUs09() {
        Usuario otroAdmin = cuentaDelPersonal(5L, RolNombres.ADMINISTRADOR_DUBSS, true);
        when(usuarioRepository.bloquearAdministradoresActivos()).thenReturn(List.of(otroAdmin));

        assertThatThrownBy(() -> service.desactivar(adminCodigo, otroAdmin.getCodigo()))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_09));
        verify(otroAdmin, never()).setActivo(false);
        verifyNoInteractions(sesionService);
    }

    @Test
    void desactivar_aUnAdministradorHabiendoOtroActivo_esValido() {
        Usuario otroAdmin = cuentaDelPersonal(5L, RolNombres.ADMINISTRADOR_DUBSS, true);
        when(usuarioRepository.bloquearAdministradoresActivos()).thenReturn(List.of(otroAdmin, admin));

        service.desactivar(adminCodigo, otroAdmin.getCodigo());

        verify(otroAdmin).setActivo(false);
    }

    @Test
    void desactivar_deUnaCuentaDePostulante_lanzaErrUs06() {
        UUID codigo = UUID.randomUUID();
        when(usuarioRepository.findPersonalByCodigo(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivar(adminCodigo, codigo))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_06));
    }

    @Test
    void reactivar_unaCuentaDesactivada_laMarcaActivaYConservaSuRol() {
        Usuario cuenta = cuentaDelPersonal(5L, RolNombres.DIRECTIVO, false);

        ResultadoAuditado<UsuarioPersonalResponse> resultado = service.reactivar(adminCodigo, cuenta.getCodigo());

        verify(cuenta).setActivo(true);
        verify(cuenta, never()).setRol(any());
        verifyNoInteractions(sesionService);
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.datos().rol()).isEqualTo(RolNombres.DIRECTIVO);
    }

    @Test
    void reactivar_unaCuentaYaActiva_lanzaErrUs07() {
        Usuario cuenta = cuentaDelPersonal(5L, RolNombres.DIRECTIVO, true);

        assertThatThrownBy(() -> service.reactivar(adminCodigo, cuenta.getCodigo()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UsuarioErrorCodes.ERR_US_07));
        verify(cuenta, never()).setActivo(true);
    }

    // ---- ayudas ----

    private CrearUsuarioPersonalRequest nuevaCuenta(String rol) {
        return new CrearUsuarioPersonalRequest("Ana", "Perez", "1234567", "ana@uagrm.edu.bo", rol);
    }

    private Pageable pageableEnviado() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioRepository).buscarPersonal(any(), any(), any(), captor.capture());
        return captor.getValue();
    }

    /** Una cuenta que el repositorio devuelve como cuenta del personal. */
    private Usuario cuentaDelPersonal(long id, String rol, boolean activo) {
        Usuario cuenta = cuenta(id, rol, activo, UUID.randomUUID());
        when(usuarioRepository.findPersonalByCodigo(cuenta.getCodigo())).thenReturn(Optional.of(cuenta));
        return cuenta;
    }

    private static Usuario cuenta(long id, String rol, boolean activo, UUID codigo) {
        Usuario cuenta = mock(Usuario.class);
        Rol rolCuenta = mock(Rol.class);
        when(rolCuenta.getNombre()).thenReturn(rol);
        when(cuenta.getId()).thenReturn(id);
        when(cuenta.getCodigo()).thenReturn(codigo);
        when(cuenta.getRol()).thenReturn(rolCuenta);
        when(cuenta.isActivo()).thenReturn(activo);
        return cuenta;
    }
}
