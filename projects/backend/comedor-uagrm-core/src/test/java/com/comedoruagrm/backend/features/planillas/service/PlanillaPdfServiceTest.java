package com.comedoruagrm.backend.features.planillas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository.Becado;
import com.comedoruagrm.backend.features.planillas.service.PlanillaPdfService.DatosPlanilla;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Genera el PDF de la planilla de verdad con JasperReports (sin base de datos ni MinIO). */
class PlanillaPdfServiceTest {

    private final PlanillaPdfService service = new PlanillaPdfService();
    private final UUID convocatoria = UUID.randomUUID();

    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void escucharLogs() {
        logger = (Logger) LoggerFactory.getLogger(PlanillaPdfService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void dejarDeEscuchar() {
        logger.detachAppender(logs);
    }

    private List<Becado> becados(int cantidad) {
        List<Becado> filas = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            filas.add(new Becado("Lucía Ñ" + i, "Peña Añez", "98" + String.format("%05d", i), "Contaduría"));
        }
        return filas;
    }

    private DatosPlanilla datos(List<Becado> becados, LocalDate desde, LocalDate hasta, boolean borrador) {
        return new DatosPlanilla(convocatoria, "Beca Comedor 2026-II", "2026-II", desde, hasta, becados,
                "Admin DUBSS", Instant.parse("2026-11-16T15:30:00Z"), borrador);
    }

    /** Con `-Dplanilla.pdf.dir=carpeta` deja el PDF en esa carpeta para revisar el diseño a ojo. */
    private static void guardarSiSePide(byte[] pdf, String nombre) throws IOException {
        String carpeta = System.getProperty("planilla.pdf.dir");
        if (carpeta != null) {
            Files.write(Path.of(carpeta, nombre), pdf);
        }
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
    void generar_devuelveUnPdfConElPeriodoLosBecadosYLosDatosDeLaConvocatoria() throws IOException {
        byte[] pdf = service.generar(datos(becados(3), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), false));
        guardarSiSePide(pdf, "planilla.pdf");

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String texto = texto(pdf);
        assertThat(texto).contains("Planilla de asistencia", "Beca Comedor 2026-II", "2026-II",
                "01/09/2026 al 30/09/2026", "Becados vigentes: 3", "Admin DUBSS", "Peña Añez Lucía Ñ1", "9800001",
                "Contaduría", "Fotocopia de");
        assertThat(texto).doesNotContain("BORRADOR");
    }

    @Test
    void generar_dibujaUnaColumnaPorDiaDelPeriodoYNadaMas() throws IOException {
        byte[] pdf = service.generar(datos(becados(1), LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 2), false));

        String texto = texto(pdf);
        // 28/09/2026 es lunes: 28 L, 29 M, 30 M, 01 J, 02 V.
        assertThat(texto).contains("28", "29", "30", "01", "02").doesNotContain("03", "27");
    }

    @Test
    void etiquetaDia_poneElDiaYLaInicial_yVaciaFueraDelPeriodo() {
        LocalDate desde = LocalDate.of(2026, 9, 28);
        LocalDate hasta = LocalDate.of(2026, 9, 30);

        assertThat(PlanillaPdfService.etiquetaDia(desde, hasta, 1)).isEqualTo("28\nL");
        assertThat(PlanillaPdfService.etiquetaDia(desde, hasta, 3)).isEqualTo("30\nM");
        assertThat(PlanillaPdfService.etiquetaDia(desde, hasta, 4)).isEmpty();
    }

    @Test
    void generar_conTreintaYUnDiasYMuchosBecados_paginaYEntra() throws IOException {
        byte[] pdf = service.generar(datos(becados(120), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), false));
        guardarSiSePide(pdf, "planilla-grande.pdf");

        PdfReader lector = new PdfReader(pdf);
        try {
            assertThat(lector.getNumberOfPages()).isGreaterThan(1);
            var hoja = lector.getPageSizeWithRotation(1);
            assertThat(hoja.getWidth()).isGreaterThan(hoja.getHeight());
        } finally {
            lector.close();
        }
        assertThat(texto(pdf)).contains("Becados vigentes: 120", "31");
    }

    @Test
    void generar_enBorrador_marcaElTitulo() throws IOException {
        byte[] pdf = service.generar(datos(becados(1), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), true));

        assertThat(texto(pdf)).contains("BORRADOR");
    }

    @Test
    void generar_conPlantillaInexistente_logueaSinDatosPersonalesYLanzaIllegalState() {
        PlanillaPdfService roto = new PlanillaPdfService("/reports/no_existe.jrxml");

        assertThatThrownBy(() -> roto.generar(datos(becados(1), LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 5), false))).isInstanceOf(IllegalStateException.class);

        assertThat(logs.list).hasSize(1);
        String registro = logs.list.get(0).getFormattedMessage() + logs.list.get(0).getArgumentArray()[0];
        assertThat(registro).doesNotContain("9800001", "Peña");
    }
}
