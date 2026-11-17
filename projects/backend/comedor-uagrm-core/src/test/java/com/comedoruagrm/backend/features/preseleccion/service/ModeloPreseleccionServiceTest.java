package com.comedoruagrm.backend.features.preseleccion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.preseleccion.dto.GuardarModeloRequest;
import com.comedoruagrm.backend.features.preseleccion.dto.ModeloPreseleccionResponse;
import com.comedoruagrm.backend.features.preseleccion.dto.ParametroModeloRequest;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository.ModeloFila;
import com.comedoruagrm.backend.features.preseleccion.repository.ModeloPreseleccionConsultaRepository.ParametroFila;
import com.comedoruagrm.backend.features.preseleccion.service.ModeloPreseleccionService.PaginaModelos;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

class ModeloPreseleccionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T15:00:00Z");

    private final ModeloPreseleccionConsultaRepository consulta = mock(ModeloPreseleccionConsultaRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final ModeloPreseleccionService service = new ModeloPreseleccionService(consulta, usuarios);

    private final UUID actorCodigo = UUID.randomUUID();
    private final Usuario actor = mock(Usuario.class);

    private static ParametroModeloRequest p(String clave, String ponderacion) {
        return new ParametroModeloRequest(clave, ponderacion == null ? null : new BigDecimal(ponderacion));
    }

    /** Las diez variables (22, 14, 10, 12, 8, 10, 8, 6, 6, 4 = 100): las seis del mockup y las del formulario oficial. */
    private static List<ParametroModeloRequest> validos() {
        return new ArrayList<>(List.of(p("ingresos_familiares", "22"), p("dependientes", "14"),
                p("procedencia", "10"), p("patrimonio", "12"), p("estado_vulnerabilidad", "8"),
                p("promedio_academico", "10"), p("dependencia_economica", "8"), p("ocupacion_sosten", "6"),
                p("discapacidad", "6"), p("hijos", "4")));
    }

    private static String codigoDe(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    // ---- validar (A1) ----

    @Test
    void validar_conLasDiezVariablesQueSuman100_devuelveLasPonderacionesEnElOrdenDelModelo() {
        var parametros = ModeloPreseleccionService.validar(new GuardarModeloRequest(null, validos()));

        assertThat(parametros.keySet()).containsExactly("ingresos_familiares", "dependientes", "procedencia",
                "patrimonio", "estado_vulnerabilidad", "promedio_academico", "dependencia_economica",
                "ocupacion_sosten", "discapacidad", "hijos");
        assertThat(parametros.get("ingresos_familiares")).isEqualByComparingTo("22");
    }

    @Test
    void validar_unaVersionConSoloLasSeisVariablesOriginales_dicePorCualesFaltan() {
        List<ParametroModeloRequest> seis = validos().subList(0, 6);

        assertThatThrownBy(() -> ModeloPreseleccionService.validar(new GuardarModeloRequest(null, seis)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("falta Dependencia económica").hasMessageContaining("falta Hijos");
    }

    @Test
    void validar_siNoSuman100_lanzaErrMod01ConLaSuma() {
        List<ParametroModeloRequest> lista = validos();
        lista.set(0, p("ingresos_familiares", "30"));

        assertThatThrownBy(() -> ModeloPreseleccionService.validar(new GuardarModeloRequest(null, lista)))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("ERR_MOD_01"))
                .hasMessageContaining("suman 108");
    }

    @Test
    void validar_conUnaVariableFaltante_dicePorCualFalta() {
        List<ParametroModeloRequest> lista = validos();
        lista.remove(5);

        assertThatThrownBy(() -> ModeloPreseleccionService.validar(new GuardarModeloRequest(null, lista)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("falta Promedio académico");
    }

    @Test
    void validar_conUnaVariableRepetidaODesconocida_loRechaza() {
        List<ParametroModeloRequest> repetida = validos();
        repetida.add(p("dependientes", "0"));
        List<ParametroModeloRequest> desconocida = validos();
        desconocida.add(p("altura", "0"));

        assertThatThrownBy(() -> ModeloPreseleccionService.validar(new GuardarModeloRequest(null, repetida)))
                .hasMessageContaining("Dependientes está repetida");
        assertThatThrownBy(() -> ModeloPreseleccionService.validar(new GuardarModeloRequest(null, desconocida)))
                .hasMessageContaining("variable desconocida «altura»");
    }

    @Test
    void validar_conPonderacionesFueraDeRangoNulasOConMuchosDecimales_juntaTodosLosErrores() {
        List<ParametroModeloRequest> lista = validos();
        lista.set(0, p("ingresos_familiares", "-1"));
        lista.set(1, p("dependientes", "101"));
        lista.set(2, p("procedencia", null));
        lista.set(3, p("patrimonio", "12.123"));

        assertThatThrownBy(() -> ModeloPreseleccionService.validar(new GuardarModeloRequest(null, lista)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Ingresos familiares debe estar entre 0 y 100")
                .hasMessageContaining("Dependientes debe estar entre 0 y 100")
                .hasMessageContaining("Procedencia no tiene ponderación")
                .hasMessageContaining("Patrimonio admite como máximo 2 decimales");
    }

    @Test
    void validar_sinParametros_oSinCuerpo_lanzaErrMod01() {
        assertThatThrownBy(() -> ModeloPreseleccionService.validar(new GuardarModeloRequest(null, null)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ModeloPreseleccionService.validar(null)).isInstanceOf(ValidationException.class);
    }

    // ---- guardar ----

    private ModeloFila fila(long id, String version, boolean activo) {
        return new ModeloFila(id, UUID.randomUUID(), version, "Ajuste", activo, AHORA);
    }

    @Test
    void guardar_archivaLaVigenteCreaLaVersionSiguienteConSusParametrosYAuditaLaAccion() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(actor.getId()).thenReturn(3L);
        when(actor.getCodigo()).thenReturn(actorCodigo);
        when(consulta.versiones()).thenReturn(List.of("v1.2", "v1.3"));
        ModeloFila nueva = fila(11L, "v1.4", true);
        when(consulta.crear(3L, "v1.4", "Ajuste")).thenReturn(nueva);
        when(consulta.parametros(11L)).thenReturn(List.of(new ParametroFila("dependientes", new BigDecimal("20.00"))));

        ResultadoAuditado<ModeloPreseleccionResponse> resultado = service.guardar(actorCodigo,
                new GuardarModeloRequest("  Ajuste  ", validos()));

        verify(consulta).archivarVigente();
        verify(consulta).insertarParametro(11L, "ingresos_familiares", new BigDecimal("22.00"));
        verify(consulta).insertarParametro(11L, "promedio_academico", new BigDecimal("10.00"));
        verify(consulta).insertarParametro(11L, "hijos", new BigDecimal("4.00"));
        assertThat(resultado.datos().version()).isEqualTo("v1.4");
        assertThat(resultado.datos().vigente()).isTrue();
        assertThat(resultado.datos().parametros()).extracting("nombre").containsExactly("Dependientes");
        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.actorId()).isEqualTo(3L);
        assertThat(resultado.entidadId()).isEqualTo(11L);
        assertThat(resultado.detalle()).containsEntry("version", "v1.4").containsKey("parametros");
    }

    @Test
    void guardar_conParametrosInvalidos_noTocaLaBase() {
        assertThatThrownBy(() -> service.guardar(actorCodigo, new GuardarModeloRequest(null, List.of())))
                .isInstanceOf(ValidationException.class);

        verify(consulta, never()).archivarVigente();
        verify(consulta, never()).crear(anyLong(), anyString(), any());
    }

    @Test
    void guardar_siOtraVersionSeGuardoAlMismoTiempo_lanzaErrMod07() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(actor.getId()).thenReturn(3L);
        when(consulta.versiones()).thenReturn(List.of("v1.3"));
        when(consulta.crear(anyLong(), anyString(), any())).thenThrow(new DuplicateKeyException("uq version"));

        assertThatThrownBy(() -> service.guardar(actorCodigo, new GuardarModeloRequest(null, validos())))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("ERR_MOD_07"));
        verify(consulta, never()).insertarParametro(anyLong(), anyString(), any());
    }

    @Test
    void guardar_sinDescripcion_laGuardaNula() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(actor.getId()).thenReturn(3L);
        when(consulta.versiones()).thenReturn(List.of());
        when(consulta.crear(3L, "v1.0", null)).thenReturn(new ModeloFila(1L, UUID.randomUUID(), "v1.0", null, true,
                AHORA));
        when(consulta.parametros(1L)).thenReturn(List.of());

        var resultado = service.guardar(actorCodigo, new GuardarModeloRequest("   ", validos()));

        assertThat(resultado.datos().version()).isEqualTo("v1.0");
    }

    // ---- lecturas ----

    @Test
    void vigente_sinVersionGuardada_devuelveNulo() {
        when(consulta.vigente()).thenReturn(Optional.empty());

        assertThat(service.vigente()).isNull();
    }

    @Test
    void vigente_devuelveLaVersionConSusParametrosYElNombreParaMostrar() {
        ModeloFila vigente = fila(5L, "v1.3", true);
        when(consulta.vigente()).thenReturn(Optional.of(vigente));
        when(consulta.parametros(5L)).thenReturn(List.of(new ParametroFila("procedencia", new BigDecimal("15.00")),
                new ParametroFila("otra", new BigDecimal("1.00"))));

        ModeloPreseleccionResponse respuesta = service.vigente();

        assertThat(respuesta.version()).isEqualTo("v1.3");
        assertThat(respuesta.parametros()).extracting("nombre").containsExactly("Procedencia", "otra");
    }

    @Test
    void listar_calculaElMetaYElDesplazamiento() {
        when(consulta.contar()).thenReturn(45L);
        when(consulta.listar(20, 20L)).thenReturn(List.of(fila(2L, "v1.2", false)));

        PaginaModelos pagina = service.listar(2, 20);

        assertThat(pagina.datos()).hasSize(1);
        assertThat(pagina.datos().get(0).vigente()).isFalse();
        assertThat(pagina.meta().totalPages()).isEqualTo(3);
        assertThat(pagina.meta().totalItems()).isEqualTo(45);
    }

    @Test
    void listar_conPaginaInvalida_lanzaValidacion() {
        assertThatThrownBy(() -> service.listar(0, 20)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.listar(1, 0)).isInstanceOf(ValidationException.class);
    }
}
