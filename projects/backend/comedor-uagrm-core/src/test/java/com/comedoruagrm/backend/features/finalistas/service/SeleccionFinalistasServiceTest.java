package com.comedoruagrm.backend.features.finalistas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.BusinessException;
import com.comedoruagrm.backend.common.exception.ConflictException;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.finalistas.model.Finalista;
import com.comedoruagrm.backend.features.finalistas.repository.FinalistaRepository;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionFinalistasResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.SeleccionarFinalistasRequest;
import com.comedoruagrm.backend.features.postulaciones.exception.AdjudicacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.CandidataFinalista;
import com.comedoruagrm.backend.features.postulaciones.repository.AdjudicacionConsultaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.service.AdjudicacionService;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Seleccion de finalistas (CU05) con el service de postulaciones y el repository simulados. */
class SeleccionFinalistasServiceTest {

    private final AdjudicacionService adjudicacion = mock(AdjudicacionService.class);
    private final FinalistaRepository finalistas = mock(FinalistaRepository.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final SeleccionFinalistasService service = new SeleccionFinalistasService(adjudicacion, finalistas,
            usuarios);

    private final UUID actorCodigo = UUID.randomUUID();
    private final UUID convocatoriaCodigo = UUID.randomUUID();
    private final Usuario actor = usuario(7L);

    private static Usuario usuario(long id) {
        Usuario u = new Usuario(null, "1234" + id, "admin@uagrm.edu.bo", "hash", "Admin", "Perez");
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "codigo", UUID.randomUUID());
        return u;
    }

    private DatosConvocatoria convocatoria(Integer cantidadConfigurada) {
        return new DatosConvocatoria(9L, convocatoriaCodigo, "Conv", "CERRADA", 5, new BigDecimal("60"),
                cantidadConfigurada);
    }

    /** Convocatoria cerrada con cupos y minimo, `disponibles` calificadas sin finalista y sus candidatas. */
    private void escenario(DatosConvocatoria conv, long disponibles, List<CandidataFinalista> candidatas) {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(adjudicacion.convocatoriaBloqueada(convocatoriaCodigo)).thenReturn(conv);
        when(adjudicacion.decisionesPosibles(conv)).thenReturn(true);
        when(adjudicacion.resultadosPublicados(9L)).thenReturn(false);
        when(adjudicacion.contarCalificadasSinFinalista(9L)).thenReturn(disponibles);
        when(adjudicacion.siguientesSinFinalista(anyLong(), anyInt())).thenAnswer(i -> {
            int limite = i.getArgument(1);
            return candidatas.subList(0, Math.min(limite, candidatas.size()));
        });
        when(finalistas.saveAndFlush(any(Finalista.class))).thenAnswer(i -> {
            Finalista f = i.getArgument(0);
            ReflectionTestUtils.setField(f, "id", 100L + f.getPosicion());
            ReflectionTestUtils.setField(f, "codigo", UUID.randomUUID());
            return f;
        });
    }

    private static List<CandidataFinalista> candidatas(int cantidad, int desdePosicion) {
        return java.util.stream.IntStream.range(0, cantidad)
                .mapToObj(i -> new CandidataFinalista(30L + i, UUID.randomUUID(), desdePosicion + i)).toList();
    }

    private void falla(Runnable accion, Class<? extends BusinessException> tipo, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOf(tipo).satisfies(e -> assertThat(((BusinessException) e)
                .getErrorCode()).isEqualTo(codigo));
        verify(finalistas, never()).saveAndFlush(any());
    }

    @Test
    void seleccionar_conConvocatoriaInexistente_lanzaNotFoundErrPri01() {
        when(usuarios.obtenerPorCodigo(actorCodigo)).thenReturn(actor);
        when(adjudicacion.convocatoriaBloqueada(convocatoriaCodigo)).thenThrow(new NotFoundException(
                PriorizacionErrorCodes.ERR_PRI_01, "La convocatoria no existe"));

        falla(() -> service.seleccionar(actorCodigo, convocatoriaCodigo, new SeleccionarFinalistasRequest(3)),
                NotFoundException.class, PriorizacionErrorCodes.ERR_PRI_01);
    }

    @Test
    void seleccionar_conConvocatoriaNoCerradaOSinCuposOMinimo_lanzaErrAdj05() {
        escenario(convocatoria(null), 10, candidatas(10, 1));
        when(adjudicacion.decisionesPosibles(any())).thenReturn(false);

        falla(() -> service.seleccionar(actorCodigo, convocatoriaCodigo, new SeleccionarFinalistasRequest(3)),
                ConflictException.class, AdjudicacionErrorCodes.ERR_ADJ_05);
    }

    @Test
    void seleccionar_conResultadosPublicados_lanzaErrAdj01() {
        escenario(convocatoria(null), 10, candidatas(10, 1));
        when(adjudicacion.resultadosPublicados(9L)).thenReturn(true);

        falla(() -> service.seleccionar(actorCodigo, convocatoriaCodigo, new SeleccionarFinalistasRequest(3)),
                ConflictException.class, AdjudicacionErrorCodes.ERR_ADJ_01);
    }

    @Test
    void seleccionar_conLaConvocatoriaYaPublicada_lanzaErrAdj01EnVezDeErrAdj05() {
        // Publicada deja de ser «cerrada»: el error correcto sigue siendo ERR_ADJ_01.
        escenario(convocatoria(null), 10, candidatas(10, 1));
        when(adjudicacion.decisionesPosibles(any())).thenReturn(false);
        when(adjudicacion.resultadosPublicados(9L)).thenReturn(true);

        falla(() -> service.seleccionar(actorCodigo, convocatoriaCodigo, new SeleccionarFinalistasRequest(3)),
                ConflictException.class, AdjudicacionErrorCodes.ERR_ADJ_01);
    }

    @Test
    void seleccionar_laCantidadDelRequestPrevaleceSobreLaConfigurada() {
        escenario(convocatoria(8), 10, candidatas(10, 1));

        ResultadoAuditado<SeleccionFinalistasResponse> r = service.seleccionar(actorCodigo, convocatoriaCodigo,
                new SeleccionarFinalistasRequest(3));

        assertThat(r.datos().seleccionados()).isEqualTo(3);
        assertThat(r.datos().finalistas()).hasSize(3);
        verify(adjudicacion).siguientesSinFinalista(9L, 3);
    }

    @Test
    void seleccionar_sinCantidadEnElRequest_usaLaConfigurada() {
        escenario(convocatoria(4), 10, candidatas(10, 1));

        ResultadoAuditado<SeleccionFinalistasResponse> r = service.seleccionar(actorCodigo, convocatoriaCodigo,
                new SeleccionarFinalistasRequest(null));

        assertThat(r.datos().seleccionados()).isEqualTo(4);
    }

    @Test
    void seleccionar_sinCantidadNiConfigurada_lanzaValidationErrSys01() {
        escenario(convocatoria(null), 10, candidatas(10, 1));

        falla(() -> service.seleccionar(actorCodigo, convocatoriaCodigo, new SeleccionarFinalistasRequest(null)),
                ValidationException.class, "ERR_SYS_01");
    }

    @Test
    void seleccionar_conCantidadCeroONegativa_lanzaValidationYNoGuardaNada() {
        escenario(convocatoria(null), 10, candidatas(10, 1));

        for (int cantidad : new int[] {0, -2}) {
            falla(() -> service.seleccionar(actorCodigo, convocatoriaCodigo,
                    new SeleccionarFinalistasRequest(cantidad)), ValidationException.class, "ERR_SYS_01");
        }
    }

    @Test
    void seleccionar_conCantidadMayorQueLasDisponibles_lanzaValidationConLaCantidadDisponible() {
        escenario(convocatoria(null), 4, candidatas(4, 1));

        assertThatThrownBy(() -> service.seleccionar(actorCodigo, convocatoriaCodigo,
                new SeleccionarFinalistasRequest(5))).isInstanceOf(ValidationException.class)
                .hasMessageContaining("4 disponibles");
        verify(finalistas, never()).saveAndFlush(any());
    }

    @Test
    void seleccionar_conCantidadMayorQueLosCupos_esPermitida() {
        // La convocatoria tiene 5 cupos; los 8 elegidos incluyen suplentes.
        escenario(convocatoria(null), 20, candidatas(20, 1));

        ResultadoAuditado<SeleccionFinalistasResponse> r = service.seleccionar(actorCodigo, convocatoriaCodigo,
                new SeleccionarFinalistasRequest(8));

        assertThat(r.datos().seleccionados()).isEqualTo(8);
    }

    @Test
    void seleccionar_guardaUnFinalistaPorCandidataConLaPosicionDeLaListaCompleta() {
        // Las posiciones 1 y 2 ya son finalistas: las siguientes ocupan 3, 4 y 5 de la lista completa.
        escenario(convocatoria(null), 3, candidatas(3, 3));

        ResultadoAuditado<SeleccionFinalistasResponse> r = service.seleccionar(actorCodigo, convocatoriaCodigo,
                new SeleccionarFinalistasRequest(3));

        ArgumentCaptor<Finalista> guardados = ArgumentCaptor.forClass(Finalista.class);
        verify(finalistas, org.mockito.Mockito.times(3)).saveAndFlush(guardados.capture());
        assertThat(guardados.getAllValues()).extracting(Finalista::getPosicion).containsExactly(3, 4, 5);
        assertThat(guardados.getAllValues()).extracting(Finalista::getPostulacionId).containsExactly(30L, 31L, 32L);
        assertThat(r.datos().convocatoria()).isEqualTo(convocatoriaCodigo);
        assertThat(r.datos().finalistas()).extracting(f -> f.posicion()).containsExactly(3L, 4L, 5L);
        assertThat(r.datos().finalistas()).extracting(f -> f.codigo()).doesNotContainNull();
    }

    @Test
    void seleccionar_unaSegundaSeleccionNoRepiteAlosAnterioresYContinuaLasPosiciones() {
        // Primera vuelta: toma las 2 primeras de 5.
        List<CandidataFinalista> todas = candidatas(5, 1);
        escenario(convocatoria(null), 5, todas);
        service.seleccionar(actorCodigo, convocatoriaCodigo, new SeleccionarFinalistasRequest(2));
        // Segunda vuelta: la consulta ya excluye a esas dos y entrega las siguientes con su posicion real.
        escenario(convocatoria(null), 3, todas.subList(2, 5));

        ResultadoAuditado<SeleccionFinalistasResponse> r = service.seleccionar(actorCodigo, convocatoriaCodigo,
                new SeleccionarFinalistasRequest(2));

        assertThat(r.datos().finalistas()).extracting(f -> f.posicion()).containsExactly(3L, 4L);
    }

    @Test
    void seleccionar_sinCandidatas_lanzaValidationYNoGuardaNada() {
        escenario(convocatoria(5), 0, List.of());

        falla(() -> service.seleccionar(actorCodigo, convocatoriaCodigo, new SeleccionarFinalistasRequest(null)),
                ValidationException.class, "ERR_SYS_01");
    }

    @Test
    void seleccionar_laBitacoraLlevaLaConvocatoriaYLaCantidadSinPuntajesNiNombres() {
        escenario(convocatoria(null), 5, candidatas(5, 1));

        ResultadoAuditado<SeleccionFinalistasResponse> r = service.seleccionar(actorCodigo, convocatoriaCodigo,
                new SeleccionarFinalistasRequest(2));

        assertThat(r.detalle()).containsEntry("convocatoria", convocatoriaCodigo.toString())
                .containsEntry("seleccionados", 2);
        assertThat(r.detalle().toString()).doesNotContain("Admin").doesNotContain("Perez").doesNotContain("puntaje");
        assertThat(r.actorId()).isEqualTo(7L);
        assertThat(r.auditar()).isTrue();
    }

    @Test
    void seleccionar_sinCuerpoUsaLaCantidadConfigurada() {
        escenario(convocatoria(2), 5, candidatas(5, 1));

        ResultadoAuditado<SeleccionFinalistasResponse> r = service.seleccionar(actorCodigo, convocatoriaCodigo,
                new SeleccionarFinalistasRequest(null));

        assertThat(r.datos().seleccionados()).isEqualTo(2);
    }
}
