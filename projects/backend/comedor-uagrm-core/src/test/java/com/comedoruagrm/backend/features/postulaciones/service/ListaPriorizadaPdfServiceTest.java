package com.comedoruagrm.backend.features.postulaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.dto.CarreraPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.dto.ExportarListaRequest;
import com.comedoruagrm.backend.features.postulaciones.dto.PostulacionPriorizadaResponse;
import com.comedoruagrm.backend.features.postulaciones.repository.ListaPriorizadaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.ListaPriorizadaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.postulaciones.service.ListaPriorizadaPdfService.PdfGenerado;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

/** Genera PDF de verdad con JasperReports (sin base de datos: el repositorio es simulado). */
class ListaPriorizadaPdfServiceTest {

    private final ListaPriorizadaRepository repository = mock(ListaPriorizadaRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final ListaPriorizadaPdfService service = new ListaPriorizadaPdfService(repository, usuarioService);

    private final UUID convocatoria = UUID.randomUUID();
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        Usuario admin = new Usuario(mock(Rol.class), "0000001", "admin@dubss.com", "hash", "Administrador", "DUBSS");
        when(usuarioService.obtenerPorCodigo(actor)).thenReturn(admin);
        when(repository.buscarConvocatoria(convocatoria))
                .thenReturn(Optional.of(new DatosConvocatoria(5L, "Beca Comedor 2026-II", "2026-II")));
    }

    private List<PostulacionPriorizadaResponse> filas(long desde, int cantidad) {
        List<PostulacionPriorizadaResponse> filas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            long posicion = desde + i + 1;
            filas.add(new PostulacionPriorizadaResponse(UUID.randomUUID(), posicion, "Lucía Ñ" + posicion,
                    "Peña Añez", "98" + posicion, new CarreraPriorizadaResponse(UUID.randomUUID(), "Contaduría"),
                    new BigDecimal("90.5"), Instant.parse("2026-09-01T10:00:00Z"), "calificada"));
        }
        return filas;
    }

    @Test
    void exportar_generaUnPdfConTodasLasFilasLeidasPorTramos() {
        int total = ListaPriorizadaPdfService.TRAMO * 2 + 300;
        when(repository.contar(convocatoria, null, null)).thenReturn((long) total);
        when(repository.buscar(eq(convocatoria), any(), any(), anyInt(), anyLong())).thenAnswer(invocacion -> {
            long desplazamiento = invocacion.getArgument(4);
            int restantes = (int) Math.max(0, total - desplazamiento);
            return filas(desplazamiento, Math.min(restantes, ListaPriorizadaPdfService.TRAMO));
        });

        ResultadoAuditado<PdfGenerado> resultado = service.exportar(actor,
                new ExportarListaRequest(convocatoria, null, null));

        byte[] pdf = resultado.datos().contenido();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(10_000);
        // 2 300 filas leidas en tramos de 1 000: tres tramos con datos y uno vacio al final.
        verify(repository, times(4)).buscar(eq(convocatoria), any(), any(),
                eq(ListaPriorizadaPdfService.TRAMO), anyLong());
        assertThat(resultado.datos().nombreArchivo()).startsWith("lista-priorizada-beca-comedor-2026-ii-")
                .endsWith(".pdf");
    }

    @Test
    void exportar_dejaLaAuditoriaConLaConvocatoriaLosFiltrosYLaCantidadPeroSinDatosPersonales() {
        UUID carrera = UUID.randomUUID();
        when(repository.buscarNombreCarrera(carrera)).thenReturn(Optional.of("Contaduría"));
        when(repository.contar(eq(convocatoria), eq(carrera), eq("%xq7%"))).thenReturn(2L);
        when(repository.buscar(eq(convocatoria), eq(carrera), eq("%xq7%"), anyInt(), anyLong()))
                .thenReturn(filas(0, 2), List.of());

        ResultadoAuditado<PdfGenerado> resultado = service.exportar(actor,
                new ExportarListaRequest(convocatoria, carrera, "xq7"));

        assertThat(resultado.auditar()).isTrue();
        assertThat(resultado.entidadId()).isEqualTo(5L);
        assertThat(resultado.detalle()).containsEntry("convocatoria", convocatoria.toString())
                .containsEntry("carreraId", carrera.toString())
                .containsEntry("conBusqueda", true)
                .containsEntry("filas", 2L);
        // El texto buscado (podria ser un CI) y los nombres no se anotan.
        assertThat(resultado.detalle().toString()).doesNotContain("xq7").doesNotContain("Lucía");
    }

    @Test
    void exportar_conLaListaVacia_da400ErrPri03SinGenerarNiAuditar() {
        when(repository.contar(convocatoria, null, null)).thenReturn(0L);

        assertThatThrownBy(() -> service.exportar(actor, new ExportarListaRequest(convocatoria, null, null)))
                .isInstanceOf(ValidationException.class).extracting("errorCode").isEqualTo("ERR_PRI_03");
        verify(repository, times(0)).buscar(any(), any(), any(), anyInt(), anyLong());
    }

    @Test
    void exportar_conUnaConvocatoriaInexistente_da404ErrPri01() {
        UUID otra = UUID.randomUUID();
        when(repository.buscarConvocatoria(otra)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exportar(actor, new ExportarListaRequest(otra, null, null)))
                .isInstanceOf(NotFoundException.class).extracting("errorCode").isEqualTo("ERR_PRI_01");
    }

    @Test
    void exportar_unaFallaDeLosDatosSubeComoErrorYNoDejaResultado() {
        when(repository.contar(convocatoria, null, null)).thenReturn(10L);
        when(repository.buscar(eq(convocatoria), any(), any(), anyInt(), anyLong()))
                .thenThrow(new IllegalStateException("falla de la base"));

        assertThatThrownBy(() -> service.exportar(actor, new ExportarListaRequest(convocatoria, null, null)))
                .isInstanceOf(RuntimeException.class);
        verify(repository, atLeastOnce()).buscar(any(), any(), any(), anyInt(), anyLong());
    }

    @Test
    void nombreArchivo_quitaTildesYSimbolosYAgregaLaFecha() {
        String nombre = ListaPriorizadaPdfService.nombreArchivo("[PRUEBA] Beca Comedor 2026-II ñandú",
                Instant.parse("2026-11-06T12:00:00Z"));

        assertThat(nombre).isEqualTo("lista-priorizada-prueba-beca-comedor-2026-ii-nandu-20261106.pdf");
    }

    @Test
    void nombreArchivo_sinLetrasUsaUnNombreGenerico() {
        assertThat(ListaPriorizadaPdfService.nombreArchivo("???", Instant.parse("2026-11-06T12:00:00Z")))
                .isEqualTo("lista-priorizada-convocatoria-20261106.pdf");
    }
}
