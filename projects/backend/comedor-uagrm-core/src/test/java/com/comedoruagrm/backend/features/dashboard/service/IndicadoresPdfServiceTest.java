package com.comedoruagrm.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.ConvocatoriaResumen;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.GestionResumen;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.IndicadorMensual;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.Totales;
import com.comedoruagrm.backend.features.dashboard.service.IndicadoresPdfService.PdfGenerado;
import com.comedoruagrm.backend.features.usuarios.model.Rol;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/** Genera el PDF del dashboard de verdad con JasperReports (sin base de datos). */
class IndicadoresPdfServiceTest {

    private final IndicadoresService indicadores = mock(IndicadoresService.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-13T15:30:00Z"), ZoneOffset.UTC);
    private final IndicadoresPdfService servicio = new IndicadoresPdfService(indicadores, usuarios, reloj);

    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        Usuario directivo = new Usuario(mock(Rol.class), "0000002", "dir@uagrm.test", "hash", "Dña. Ñusta",
                "Peña Añez");
        when(usuarios.obtenerPorCodigo(actor)).thenReturn(directivo);
    }

    private IndicadoresResponse datos(String gestion, ConvocatoriaResumen convocatoria, Instant actualizado) {
        return new IndicadoresResponse(new GestionResumen(UUID.randomUUID(), gestion), convocatoria,
                new Totales(120, 30, 12, null),
                List.of(new IndicadorMensual("2026-09", 80, 0, 0, null), new IndicadorMensual("2026-10", 40, 30, 12, null)),
                actualizado, false);
    }

    private static String texto(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder todo = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                todo.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return todo.toString();
        } finally {
            lector.close();
        }
    }

    @Test
    void generar_empiezaPorPdf_yLlevaTotalesMesesYQuienLoGenera() throws IOException {
        UUID gestion = UUID.randomUUID();
        when(indicadores.obtener(gestion, null)).thenReturn(datos("2026-II", null,
                Instant.parse("2026-11-13T06:00:00Z")));

        PdfGenerado pdf = servicio.generar(actor, gestion, null);

        assertThat(new String(pdf.contenido(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        String texto = texto(pdf.contenido());
        assertThat(texto).contains("Dashboard de indicadores").contains("Gestión: 2026-II")
                .contains("Todas las convocatorias").contains("13/11/2026 02:00")
                .contains("Postulaciones recibidas: 120").contains("Finalistas: 30")
                .contains("Becas adjudicadas: 12").contains("Asistencia promedio: Sin datos")
                .contains("2026-09").contains("2026-10")
                .contains("Generado el 13/11/2026 11:30 por Dña. Ñusta Peña Añez");
        assertThat(pdf.nombreArchivo()).isEqualTo("indicadores-2026-ii-20261113.pdf");
        String carpeta = System.getProperty("indicadores.pdf.dir");
        if (carpeta != null) {
            Files.write(Path.of(carpeta, "indicadores-ejemplo.pdf"), pdf.contenido());
        }
    }

    @Test
    void generar_respetaLaSeleccion_otraGestionYConvocatoriaDanOtroContenido() throws IOException {
        UUID g1 = UUID.randomUUID();
        UUID g2 = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        when(indicadores.obtener(g1, null)).thenReturn(datos("2026-I", null, null));
        when(indicadores.obtener(g2, c)).thenReturn(datos("2027-I", new ConvocatoriaResumen(c, "Beca ñandú"),
                Instant.parse("2026-11-13T06:00:00Z")));

        String primero = texto(servicio.generar(actor, g1, null).contenido());
        String segundo = texto(servicio.generar(actor, g2, c).contenido());

        assertThat(primero).contains("Gestión: 2026-I").contains("Aún no calculado")
                .doesNotContain("Beca ñandú");
        assertThat(segundo).contains("Gestión: 2027-I").contains("Convocatoria: Beca ñandú")
                .doesNotContain("Aún no calculado");
    }

    @Test
    void generar_sinDatos_lanzaErrDas02() {
        when(indicadores.obtener(null, null)).thenReturn(new IndicadoresResponse(
                new GestionResumen(UUID.randomUUID(), "2026-II"), null, new Totales(0, 0, 0, null), List.of(), null,
                true));

        assertThatThrownBy(() -> servicio.generar(actor, null, null)).isInstanceOf(ValidationException.class)
                .extracting("errorCode").isEqualTo("ERR_DAS_02");
    }

    @Test
    void generar_sinGestiones_tambienEsErrDas02() {
        when(indicadores.obtener(null, null)).thenReturn(
                new IndicadoresResponse(null, null, new Totales(0, 0, 0, null), List.of(), null, true));

        assertThatThrownBy(() -> servicio.generar(actor, null, null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void generar_gestionInexistente_propagaErrDas01() {
        UUID gestion = UUID.randomUUID();
        when(indicadores.obtener(gestion, null)).thenThrow(new NotFoundException("ERR_DAS_01", "no existe"));

        assertThatThrownBy(() -> servicio.generar(actor, gestion, null)).isInstanceOf(NotFoundException.class)
                .extracting("errorCode").isEqualTo("ERR_DAS_01");
    }

    @Test
    void generar_exigeRolDirectivo_enTransaccionDeSoloLectura_yNoSeAnotaEnLaBitacora() throws NoSuchMethodException {
        Method m = IndicadoresPdfService.class.getMethod("generar", UUID.class, UUID.class, UUID.class);

        assertThat(m.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('DIRECTIVO')");
        assertThat(m.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(m.getAnnotations()).noneMatch(a -> a.annotationType().getSimpleName().equals("Auditar"));
        verify(indicadores, org.mockito.Mockito.never()).obtener(null, null);
    }

    @Test
    void nombreArchivo_quitaTildesYEspacios() {
        assertThat(IndicadoresPdfService.nombreArchivo("Gestión 2026 - II", Instant.parse("2026-11-13T15:30:00Z")))
                .isEqualTo("indicadores-gestion-2026-ii-20261113.pdf");
    }
}
