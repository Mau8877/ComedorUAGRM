package com.comedoruagrm.backend.features.actas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.Adjudicado;
import com.comedoruagrm.backend.features.actas.service.ActaPdfService.DatosActa;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Genera el PDF del acta de verdad con JasperReports (sin base de datos ni MinIO). */
class ActaPdfServiceTest {

    private final ActaPdfService service = new ActaPdfService();
    private final UUID convocatoria = UUID.randomUUID();

    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void escucharLogs() {
        logger = (Logger) LoggerFactory.getLogger(ActaPdfService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void dejarDeEscuchar() {
        logger.detachAppender(logs);
    }

    private List<Adjudicado> adjudicados(int cantidad) {
        List<Adjudicado> filas = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            filas.add(new Adjudicado("Lucía Ñ" + i, "Peña Añez", "98" + String.format("%05d", i), "Contaduría",
                    new BigDecimal("90.5").subtract(new BigDecimal(i).movePointLeft(2))));
        }
        return filas;
    }

    private DatosActa datos(List<Adjudicado> adjudicados) {
        return new DatosActa(convocatoria, "Beca Comedor 2026-II", "2026-II",
                Instant.parse("2026-07-01T12:00:00Z"), Instant.parse("2026-08-31T12:00:00Z"), 50,
                new BigDecimal("60.00"), adjudicados, "Admin DUBSS", Instant.parse("2026-11-13T15:30:00Z"));
    }

    /** Con `-Dacta.pdf.dir=carpeta` deja el PDF en esa carpeta para revisar el diseño a ojo. */
    private static void guardarSiSePide(byte[] pdf, String nombre) throws IOException {
        String carpeta = System.getProperty("acta.pdf.dir");
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
    void generar_conCincoAdjudicados_empiezaPorPdfYLlevaLosDatosEnElOrdenRecibido() throws IOException {
        byte[] pdf = service.generar(datos(adjudicados(5)), "001/2026");
        guardarSiSePide(pdf, "acta-ejemplo.pdf");

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        String texto = texto(pdf);
        assertThat(texto).contains("001/2026").contains("Beca Comedor 2026-II").contains("2026-II")
                .contains("Acta de adjudicación").contains("Adjudicados: 5 de 50 cupos")
                .contains("Puntaje mínimo: 60,00").contains("13/11/2026 11:30")
                .contains("Generada por: Admin DUBSS").contains("Administrador DUBSS").contains("Autoridad DUBSS");
        int anterior = -1;
        for (int i = 1; i <= 5; i++) {
            int posicion = texto.indexOf("Lucía Ñ" + i + " Peña Añez");
            assertThat(posicion).as("nombre %d", i).isGreaterThan(anterior);
            assertThat(texto).contains("98" + String.format("%05d", i));
            anterior = posicion;
        }
        assertThat(texto).contains("90,49").doesNotContain("Sin adjudicados").doesNotContain("BORRADOR");
    }

    @Test
    void generar_sinAdjudicados_llevaLaLeyendaSinAdjudicados() throws IOException {
        String texto = texto(service.generar(datos(List.of()), "002/2026"));

        assertThat(texto).contains("Sin adjudicados").contains("002/2026").contains("Adjudicados: 0 de 50 cupos");
    }

    @Test
    void generar_elBorrador_llevaLaMarcaYElNumeroBorrador() throws IOException {
        String texto = texto(service.generar(datos(adjudicados(2)), NumeroActaService.BORRADOR));

        assertThat(texto).contains("BORRADOR").contains("sin valor oficial").contains("N.º borrador");
    }

    @Test
    void generar_noLlevaNadaFueraDeLoPrevisto() throws IOException {
        String texto = texto(service.generar(datos(adjudicados(2)), "001/2026"));

        assertThat(texto.toLowerCase()).doesNotContain("correo").doesNotContain("@").doesNotContain("motivo")
                .doesNotContain("socioecon").doesNotContain("no adjudicad");
    }

    @Test
    void generar_conCientosDeFilas_paginaSinFallar() throws IOException {
        byte[] pdf = service.generar(datos(adjudicados(400)), "003/2026");

        PdfReader lector = new PdfReader(pdf);
        try {
            assertThat(lector.getNumberOfPages()).isGreaterThan(5);
        } finally {
            lector.close();
        }
        assertThat(texto(pdf)).contains("Lucía Ñ400 Peña Añez");
    }

    @Test
    void generar_conLaPlantillaRota_lanzaIllegalStateYNoLogueaDatosPersonales() {
        ActaPdfService conPlantillaRota = new ActaPdfService("/no-existe/acta.jrxml");

        assertThatThrownBy(() -> conPlantillaRota.generar(datos(adjudicados(1)), "001/2026"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(logs.list).hasSize(1);
        ILoggingEvent evento = logs.list.get(0);
        assertThat(evento.getLevel().toString()).isEqualTo("ERROR");
        String registrado = evento.getFormattedMessage() + Arrays.toString(evento.getArgumentArray());
        assertThat(registrado).contains(convocatoria.toString()).doesNotContain("Lucía").doesNotContain("9800001");
    }
}
