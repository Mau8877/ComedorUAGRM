package com.comedoruagrm.backend.features.planillas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.comedoruagrm.backend.features.planillas.repository.PlanillasConsultaRepository.Becado;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;

/**
 * Genera el PDF imprimible de la planilla de asistencia con JasperReports (CU09).
 *
 * * Recibe los datos ya leidos (los becados vienen en una sola lista: una planilla tiene como mucho unos cientos
 * * de filas, los cupos de la convocatoria) y no toca la base ni MinIO. La plantilla se compila una sola vez.
 * * Zona `America/La_Paz` y locale `es-BO`, como el acta (CU08).
 * * La hoja es apaisada: nombre, CI, carrera, un recuadro para pegar la fotocopia del carnet y una columna de firma
 * * por cada dia del periodo (hasta {@link #MAX_DIAS}, las que la plantilla dibuja).
 * * La vista previa lleva la marca «BORRADOR» en el titulo.
 * ! Un fallo de Jasper se loguea con la convocatoria y el tipo de error, nunca con nombres ni CI, y se relanza
 * ! como `IllegalStateException` (el handler global lo responde como `500` generico).
 * ? El formato oficial (encabezado, rotulos, dias habiles) esta por definir con la DUBSS.
 */
@Service
public class PlanillaPdfService {

    private static final Logger LOG = LoggerFactory.getLogger(PlanillaPdfService.class);

    /** Columnas de dia que dibuja la plantilla; el `service` rechaza un periodo mas largo. */
    public static final int MAX_DIAS = 31;

    static final String PLANTILLA = "/reports/planilla_asistencia.jrxml";
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final Locale LOCALE = Locale.forLanguageTag("es-BO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String plantilla;

    private volatile JasperReport plantillaCompilada;

    /** Todo lo que lleva la planilla, ya leido: la convocatoria, el periodo, los becados y quien la genera. */
    public record DatosPlanilla(UUID convocatoriaCodigo, String convocatoria, String gestion, LocalDate desde,
            LocalDate hasta, List<Becado> becados, String generadaPor, Instant generadaEn, boolean borrador) {
    }

    public PlanillaPdfService() {
        this(PLANTILLA);
    }

    /** Con otra plantilla: solo para probar el fallo de la plantilla. */
    PlanillaPdfService(String plantilla) {
        this.plantilla = plantilla;
    }

    public byte[] generar(DatosPlanilla datos) {
        try {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put(JRParameter.REPORT_LOCALE, LOCALE);
            parametros.put("TITULO", datos.borrador() ? "Planilla de asistencia – BORRADOR" : "Planilla de asistencia");
            parametros.put("CONVOCATORIA", datos.convocatoria());
            parametros.put("GESTION", datos.gestion());
            parametros.put("PERIODO", FECHA.format(datos.desde()) + " al " + FECHA.format(datos.hasta()));
            parametros.put("TOTAL", "Becados vigentes: " + datos.becados().size());
            parametros.put("GENERADO_EN", FECHA_HORA.format(datos.generadaEn().atZone(ZONA)));
            parametros.put("GENERADO_POR", datos.generadaPor());
            for (int i = 1; i <= MAX_DIAS; i++) {
                parametros.put("D" + i, etiquetaDia(datos.desde(), datos.hasta(), i));
            }

            JasperPrint impreso = JasperFillManager.fillReport(plantilla(), parametros,
                    new JRMapCollectionDataSource(filas(datos.becados())));

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            JRPdfExporter exportador = new JRPdfExporter();
            exportador.setExporterInput(new SimpleExporterInput(impreso));
            exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));
            exportador.exportReport();
            return salida.toByteArray();
        } catch (JRException | RuntimeException e) {
            LOG.error("No se pudo generar el PDF de la planilla de asistencia", kv("module", "planillas"),
                    kv("convocatoria", datos.convocatoriaCodigo()), kv("error", e.getClass().getSimpleName()));
            throw new IllegalStateException("No se pudo generar el PDF de la planilla de asistencia", e);
        }
    }

    /** «dd», salto de linea y la inicial del dia de la semana (`15` / `L`), o vacio si el dia `n` no existe. */
    static String etiquetaDia(LocalDate desde, LocalDate hasta, int n) {
        LocalDate dia = desde.plusDays(n - 1L);
        if (dia.isAfter(hasta)) {
            return "";
        }
        String inicial = dia.getDayOfWeek().getDisplayName(TextStyle.NARROW, LOCALE).toUpperCase(LOCALE);
        return String.format(LOCALE, "%02d", dia.getDayOfMonth()) + "\n" + inicial;
    }

    private static List<Map<String, ?>> filas(List<Becado> becados) {
        List<Map<String, ?>> filas = new ArrayList<>(becados.size());
        int orden = 1;
        for (Becado b : becados) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("orden", String.valueOf(orden++));
            fila.put("nombre", b.apellidos() + " " + b.nombres());
            fila.put("ci", b.ci());
            fila.put("carrera", b.carrera());
            filas.add(fila);
        }
        return filas;
    }

    // La plantilla se compila una vez y se reutiliza.
    private JasperReport plantilla() throws JRException {
        JasperReport compilada = plantillaCompilada;
        if (compilada == null) {
            try (InputStream fuente = getClass().getResourceAsStream(plantilla)) {
                if (fuente == null) {
                    throw new JRException("No se encontró la plantilla " + plantilla);
                }
                compilada = JasperCompileManager.compileReport(fuente);
            } catch (IOException e) {
                throw new JRException(e);
            }
            plantillaCompilada = compilada;
        }
        return compilada;
    }
}
