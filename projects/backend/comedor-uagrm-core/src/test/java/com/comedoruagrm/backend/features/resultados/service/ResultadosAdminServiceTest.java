package com.comedoruagrm.backend.features.resultados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.features.resultados.dto.DecisionResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.PaginaResultados;
import com.comedoruagrm.backend.features.resultados.dto.ResumenPublicacionResponse;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Conteos;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosPublicacion;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Destinatario;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.FiltroDecisiones;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Pagina;

@ExtendWith(MockitoExtension.class)
class ResultadosAdminServiceTest {

    private static final long ID = 7L;

    @Mock
    private ResultadosConsultaRepository consulta;

    private ResultadosAdminService service;
    private final UUID codigo = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new ResultadosAdminService(consulta);
    }

    private void convocatoria(String estado) {
        when(consulta.convocatoria(codigo)).thenReturn(Optional.of(new DatosConvocatoria(ID, codigo, "Beca 2026", estado)));
    }

    private static Destinatario destinatario(String resultado) {
        return new Destinatario(1L, "Ana", "ana@x.test", 1L, UUID.randomUUID(), resultado);
    }

    // ---- resumen ----

    @Test
    void resumen_conTodoDecidido_puedePublicar_yTraeLosConteosYAQuienSeAvisa() {
        convocatoria("CERRADA");
        when(consulta.contarPorEstado(ID)).thenReturn(new Conteos(3, 2, 0, 6));
        when(consulta.publicacion(ID)).thenReturn(Optional.empty());
        when(consulta.destinatariosDelAviso(ID)).thenReturn(List.of(destinatario("ADJUDICADA"),
                destinatario("ADJUDICADA"), destinatario("NO_ADJUDICADA")));

        ResumenPublicacionResponse r = service.resumen(codigo);

        assertThat(r.convocatoria().codigo()).isEqualTo(codigo);
        assertThat(r.convocatoria().nombre()).isEqualTo("Beca 2026");
        assertThat(r.convocatoria().estado()).isEqualTo("CERRADA");
        assertThat(r.adjudicados()).isEqualTo(3);
        assertThat(r.noAdjudicados()).isEqualTo(2);
        assertThat(r.calificadasPendientes()).isZero();
        assertThat(r.totalFinalistas()).isEqualTo(6);
        assertThat(r.aAvisar()).isEqualTo(3);
        assertThat(r.yaPublicada()).isFalse();
        assertThat(r.publicadoEn()).isNull();
        assertThat(r.publicadoPor()).isNull();
        assertThat(r.puedePublicar()).isTrue();
    }

    @Test
    void resumen_conCalificadasPendientes_noPuedePublicar() {
        convocatoria("CERRADA");
        when(consulta.contarPorEstado(ID)).thenReturn(new Conteos(1, 0, 2, 3));
        when(consulta.publicacion(ID)).thenReturn(Optional.empty());
        when(consulta.destinatariosDelAviso(ID)).thenReturn(List.of(destinatario("ADJUDICADA")));

        ResumenPublicacionResponse r = service.resumen(codigo);

        assertThat(r.calificadasPendientes()).isEqualTo(2);
        assertThat(r.puedePublicar()).isFalse();
    }

    @Test
    void resumen_yaPublicada_traeQuienYCuando_yNoPuedePublicar() {
        Instant cuando = Instant.parse("2026-11-12T15:00:00Z");
        convocatoria("PUBLICADA");
        when(consulta.contarPorEstado(ID)).thenReturn(new Conteos(1, 1, 0, 2));
        when(consulta.publicacion(ID)).thenReturn(Optional.of(new DatosPublicacion(cuando, "Admin DUBSS")));
        when(consulta.destinatariosDelAviso(ID)).thenReturn(List.of(destinatario("ADJUDICADA"),
                destinatario("NO_ADJUDICADA")));

        ResumenPublicacionResponse r = service.resumen(codigo);

        assertThat(r.yaPublicada()).isTrue();
        assertThat(r.publicadoEn()).isEqualTo(cuando);
        assertThat(r.publicadoPor()).isEqualTo("Admin DUBSS");
        assertThat(r.puedePublicar()).isFalse();
    }

    @Test
    void resumen_conPublicacionAunqueElEstadoSigaCerrada_noPuedePublicar() {
        convocatoria("CERRADA");
        when(consulta.contarPorEstado(ID)).thenReturn(new Conteos(1, 0, 0, 1));
        when(consulta.publicacion(ID)).thenReturn(Optional.of(new DatosPublicacion(Instant.now(), "Admin")));
        when(consulta.destinatariosDelAviso(ID)).thenReturn(List.of());

        assertThat(service.resumen(codigo).puedePublicar()).isFalse();
    }

    @Test
    void resumen_convocatoriaNoCerrada_noPuedePublicar() {
        convocatoria("ABIERTA");
        when(consulta.contarPorEstado(ID)).thenReturn(new Conteos(0, 0, 0, 0));
        when(consulta.publicacion(ID)).thenReturn(Optional.empty());
        when(consulta.destinatariosDelAviso(ID)).thenReturn(List.of());

        assertThat(service.resumen(codigo).puedePublicar()).isFalse();
    }

    @Test
    void resumen_sinNingunAdjudicado_peroTodoDecidido_puedePublicar() {
        convocatoria("CERRADA");
        when(consulta.contarPorEstado(ID)).thenReturn(new Conteos(0, 4, 0, 4));
        when(consulta.publicacion(ID)).thenReturn(Optional.empty());
        when(consulta.destinatariosDelAviso(ID)).thenReturn(List.of(destinatario("NO_ADJUDICADA"),
                destinatario("NO_ADJUDICADA"), destinatario("NO_ADJUDICADA"), destinatario("NO_ADJUDICADA")));

        ResumenPublicacionResponse r = service.resumen(codigo);

        assertThat(r.adjudicados()).isZero();
        assertThat(r.aAvisar()).isEqualTo(4);
        assertThat(r.puedePublicar()).isTrue();
    }

    @Test
    void resumen_convocatoriaInexistente_lanzaNotFoundConErrPri01() {
        when(consulta.convocatoria(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resumen(codigo)).isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PRI_01"));
        verify(consulta, never()).contarPorEstado(any(Long.class));
    }

    // ---- decisiones ----

    @Test
    void decisiones_devuelveLaPaginaConSuMeta() {
        convocatoria("CERRADA");
        Pageable pageable = PageRequest.of(1, 2);
        FiltroDecisiones filtro = new FiltroDecisiones("ADJUDICADA", "ana");
        DecisionResultadoResponse fila = new DecisionResultadoResponse(UUID.randomUUID(), "Ana", "Rojas",
                "ADJUDICADA", new BigDecimal("80"), Instant.now());
        when(consulta.decisiones(eq(ID), eq(filtro), eq(pageable))).thenReturn(new Pagina<>(List.of(fila), 5));

        PaginaResultados<DecisionResultadoResponse> pagina = service.decisiones(codigo, filtro, pageable);

        assertThat(pagina.datos()).containsExactly(fila);
        assertThat(pagina.meta().page()).isEqualTo(2);
        assertThat(pagina.meta().pageSize()).isEqualTo(2);
        assertThat(pagina.meta().totalItems()).isEqualTo(5);
        assertThat(pagina.meta().totalPages()).isEqualTo(3);
    }

    @Test
    void decisiones_convocatoriaInexistente_lanzaNotFound() {
        when(consulta.convocatoria(codigo)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decisiones(codigo, new FiltroDecisiones(null, null), PageRequest.of(0, 20)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- permisos ----

    @Test
    void ambosMetodos_exigenElRolAdministradorDubss() throws NoSuchMethodException {
        PreAuthorize resumen = ResultadosAdminService.class.getMethod("resumen", UUID.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize decisiones = ResultadosAdminService.class
                .getMethod("decisiones", UUID.class, FiltroDecisiones.class, Pageable.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(resumen.value()).isEqualTo("hasRole('ADMINISTRADOR_DUBSS')");
        assertThat(decisiones.value()).isEqualTo("hasRole('ADMINISTRADOR_DUBSS')");
    }
}
