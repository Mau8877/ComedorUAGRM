package com.comedoruagrm.backend.features.finalistas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.features.finalistas.dto.FinalistaResponse;
import com.comedoruagrm.backend.features.finalistas.dto.RegistrarEntregaRequest;
import com.comedoruagrm.backend.features.finalistas.model.Finalista;
import com.comedoruagrm.backend.features.finalistas.repository.FinalistaConsultaRepository;
import com.comedoruagrm.backend.features.finalistas.repository.FinalistaRepository;
import com.comedoruagrm.backend.features.notificaciones.service.NotificacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/**
 * Pruebas de caja blanca de CU-21 (capitulo 9, 9.3.2): un test por camino basico factible de
 * `FinalistaService.registrarEntrega` (C1 a C6). El septimo camino no es factible (con la fecha de
 * hoy el nodo 5 nunca es verdadero).
 */
class FinalistaServiceCajaBlancaTest {

    private static final LocalDate HOY = LocalDate.now(FinalistaConsultaRepository.ZONA);

    private final FinalistaRepository finalistaRepository = mock(FinalistaRepository.class);
    private final FinalistaConsultaRepository consulta = mock(FinalistaConsultaRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final NotificacionService notificacionService = mock(NotificacionService.class);
    private final FinalistaService service = new FinalistaService(finalistaRepository, consulta, usuarioService,
            notificacionService);

    private final UUID finalistaCodigo = UUID.randomUUID();
    private Usuario admin;
    private Finalista finalista;

    @BeforeEach
    void preparar() {
        admin = new Usuario(null, "12341", "admin@uagrm.edu.bo", "hash", "Admin", "Perez");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "codigo", UUID.randomUUID());
        finalista = new Finalista(70L, 1);
        ReflectionTestUtils.setField(finalista, "id", 10L);
        ReflectionTestUtils.setField(finalista, "codigo", finalistaCodigo);
        when(usuarioService.obtenerPorCodigo(admin.getCodigo())).thenReturn(admin);
        when(finalistaRepository.findByCodigo(finalistaCodigo)).thenReturn(Optional.of(finalista));
        when(consulta.buscarUno(finalistaCodigo)).thenReturn(Optional.of(new FinalistaResponse(finalistaCodigo, 1,
                "Ana", "Perez", "123", null, "ENVIADA", null, null, HOY, "SIN_AUDITAR", null, "calificada")));
    }

    private ResultadoAuditado<FinalistaResponse> registrar(LocalDate fecha, String observaciones) {
        return service.registrarEntrega(admin.getCodigo(), finalistaCodigo,
                new RegistrarEntregaRequest(fecha, observaciones));
    }

    @Test
    void registrarEntrega_C1_fechaFutura_lanzaErrFin03YNoGuarda() {
        assertThatThrownBy(() -> registrar(HOY.plusDays(1), null)).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("ERR_FIN_03"));
        verify(finalistaRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarEntrega_C2_sinFecha_registraConLaFechaDeHoy() {
        ResultadoAuditado<FinalistaResponse> r = registrar(null, null);

        assertThat(r.detalle()).containsEntry("fechaRecepcion", HOY.toString())
                .doesNotContainKey("fechaAnterior").doesNotContainKey("observaciones");
        verify(finalistaRepository).saveAndFlush(finalista);
    }

    @Test
    void registrarEntrega_C3_conFechaIndicada_registraConEsaFecha() {
        LocalDate fecha = HOY.minusDays(1);

        ResultadoAuditado<FinalistaResponse> r = registrar(fecha, null);

        assertThat(r.detalle()).containsEntry("fechaRecepcion", fecha.toString());
        assertThat(finalista.getDocumentacionRecibidaEn())
                .isEqualTo(fecha.atStartOfDay(FinalistaConsultaRepository.ZONA).toInstant());
    }

    @Test
    void registrarEntrega_C4_corrigiendoUnaEntregaPrevia_elDetalleLlevaLaFechaAnterior() {
        LocalDate antes = HOY.minusDays(3);
        finalista.setDocumentacionRecibidaEn(antes.atStartOfDay(FinalistaConsultaRepository.ZONA).toInstant());

        ResultadoAuditado<FinalistaResponse> r = registrar(null, null);

        assertThat(r.detalle()).containsEntry("fechaAnterior", antes.toString());
    }

    @Test
    void registrarEntrega_C5_conObservaciones_elDetalleLasIncluyeSinEspaciosSobrantes() {
        ResultadoAuditado<FinalistaResponse> r = registrar(null, " Falta una fotocopia ");

        assertThat(r.detalle()).containsEntry("observaciones", "Falta una fotocopia");
    }

    @Test
    void registrarEntrega_C6_observacionesEnBlanco_noSeAgreganAlDetalle() {
        ResultadoAuditado<FinalistaResponse> r = registrar(null, "   ");

        assertThat(r.detalle()).doesNotContainKey("observaciones");
        verify(finalistaRepository).saveAndFlush(finalista);
    }
}
