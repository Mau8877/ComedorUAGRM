package com.comedoruagrm.backend.features.resultados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.comedoruagrm.backend.features.resultados.dto.AdjudicadoPublicoResponse;
import com.comedoruagrm.backend.features.resultados.dto.ConvocatoriaPublicadaResponse;
import com.comedoruagrm.backend.features.resultados.dto.MiResultadoResponse;
import com.comedoruagrm.backend.features.resultados.dto.PaginaResultados;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.DatosPublicacion;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.Pagina;
import com.comedoruagrm.backend.features.resultados.repository.ResultadosConsultaRepository.ResultadoPropio;

@ExtendWith(MockitoExtension.class)
class ResultadosPostulanteServiceTest {

    private static final long ID = 7L;
    private static final Instant PUBLICADO = Instant.parse("2026-11-12T15:00:00Z");

    @Mock
    private ResultadosConsultaRepository consulta;

    private ResultadosPostulanteService service;
    private final UUID convocatoria = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new ResultadosPostulanteService(consulta);
    }

    private void convocatoriaExiste() {
        when(consulta.convocatoria(convocatoria)).thenReturn(
                Optional.of(new DatosConvocatoria(ID, convocatoria, "Beca 2026-II", "PUBLICADA")));
    }

    private void publicada() {
        convocatoriaExiste();
        when(consulta.publicacion(ID)).thenReturn(Optional.of(new DatosPublicacion(PUBLICADO, "Admin DUBSS")));
    }

    private void sinPublicar() {
        convocatoriaExiste();
        when(consulta.publicacion(ID)).thenReturn(Optional.empty());
    }

    // ---- antes de publicar ----

    @Test
    void adjudicados_antesDePublicar_lanzaErrPub04_ynoConsultaLaLista() {
        sinPublicar();

        assertThatThrownBy(() -> service.adjudicados(convocatoria, null, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PUB_04"));

        verify(consulta, never()).adjudicadosPublicos(anyLong(), any(), any());
    }

    @Test
    void miResultado_antesDePublicar_lanzaErrPub04_ynoConsultaElResultado() {
        sinPublicar();

        assertThatThrownBy(() -> service.miResultado(convocatoria, usuario))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PUB_04"));

        verify(consulta, never()).miResultado(anyLong(), any());
    }

    @Test
    void adjudicadosYMiResultado_convocatoriaInexistente_lanzanErrPri01() {
        when(consulta.convocatoria(convocatoria)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjudicados(convocatoria, null, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PRI_01"));
        assertThatThrownBy(() -> service.miResultado(convocatoria, usuario))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("ERR_PRI_01"));
    }

    // ---- lista publica ----

    @Test
    void adjudicados_despuesDePublicar_devuelveLaPaginaConLosCincoCamposYSuMeta() {
        publicada();
        Pageable pageable = PageRequest.of(0, 2);
        AdjudicadoPublicoResponse ana = new AdjudicadoPublicoResponse("Ana", "Rojas", new BigDecimal("88.5"),
                "Ingeniería Informática", "Facultad de Ciencias");
        when(consulta.adjudicadosPublicos(eq(ID), eq("ana"), eq(pageable)))
                .thenReturn(new Pagina<>(List.of(ana), 3));

        PaginaResultados<AdjudicadoPublicoResponse> pagina = service.adjudicados(convocatoria, "ana", pageable);

        assertThat(pagina.datos()).containsExactly(ana);
        assertThat(pagina.meta().page()).isEqualTo(1);
        assertThat(pagina.meta().pageSize()).isEqualTo(2);
        assertThat(pagina.meta().totalItems()).isEqualTo(3);
        assertThat(pagina.meta().totalPages()).isEqualTo(2);
        assertThat(AdjudicadoPublicoResponse.class.getRecordComponents()).extracting("name")
                .containsExactly("nombres", "apellidos", "nota", "carrera", "facultad");
    }

    // ---- mi resultado ----

    @Test
    void miResultado_deUnAdjudicado_traeResultadoYNota() {
        publicada();
        when(consulta.miResultado(ID, usuario)).thenReturn(Optional.of(new ResultadoPropio("ADJUDICADA",
                new BigDecimal("90"))));

        MiResultadoResponse r = service.miResultado(convocatoria, usuario);

        assertThat(r.resultado()).isEqualTo("ADJUDICADA");
        assertThat(r.nota()).isEqualByComparingTo("90");
        assertThat(r.publicadoEn()).isEqualTo(PUBLICADO);
    }

    @Test
    void miResultado_deUnNoAdjudicado_traeElResultadoSinMotivo() {
        publicada();
        when(consulta.miResultado(ID, usuario)).thenReturn(Optional.of(new ResultadoPropio("NO_ADJUDICADA",
                new BigDecimal("40"))));

        MiResultadoResponse r = service.miResultado(convocatoria, usuario);

        assertThat(r.resultado()).isEqualTo("NO_ADJUDICADA");
        assertThat(MiResultadoResponse.class.getRecordComponents()).extracting("name")
                .containsExactly("resultado", "nota", "publicadoEn").doesNotContain("motivo");
    }

    @Test
    void miResultado_deUnUsuarioAjenoOSinDecision_devuelveResultadoNulo() {
        publicada();
        when(consulta.miResultado(ID, usuario)).thenReturn(Optional.empty());

        MiResultadoResponse r = service.miResultado(convocatoria, usuario);

        assertThat(r.resultado()).isNull();
        assertThat(r.nota()).isNull();
        assertThat(r.publicadoEn()).isEqualTo(PUBLICADO);
    }

    // ---- convocatorias publicadas ----

    @Test
    void convocatoriasPublicadas_devuelveLaPaginaConSuMeta() {
        Pageable pageable = PageRequest.of(0, 20);
        ConvocatoriaPublicadaResponse fila = new ConvocatoriaPublicadaResponse(convocatoria, "Beca 2026-II", PUBLICADO);
        when(consulta.convocatoriasPublicadas(pageable)).thenReturn(new Pagina<>(List.of(fila), 1));

        PaginaResultados<ConvocatoriaPublicadaResponse> pagina = service.convocatoriasPublicadas(pageable);

        assertThat(pagina.datos()).containsExactly(fila);
        assertThat(pagina.meta().totalItems()).isEqualTo(1);
        assertThat(pagina.meta().totalPages()).isEqualTo(1);
    }

    // ---- sesion ----

    @Test
    void lasTresConsultas_exigenSesionValida() throws NoSuchMethodException {
        assertThat(ResultadosPostulanteService.class.getMethod("convocatoriasPublicadas", Pageable.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo("isAuthenticated()");
        assertThat(ResultadosPostulanteService.class.getMethod("adjudicados", UUID.class, String.class,
                Pageable.class).getAnnotation(PreAuthorize.class).value()).isEqualTo("isAuthenticated()");
        assertThat(ResultadosPostulanteService.class.getMethod("miResultado", UUID.class, UUID.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo("isAuthenticated()");
    }
}
