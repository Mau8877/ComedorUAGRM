package com.comedoruagrm.backend.features.actas.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

import com.comedoruagrm.backend.features.actas.repository.ActasConsultaRepository.Adjudicado;

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
 * Genera el PDF del acta de adjudicacion con JasperReports (CU08).
 *
 * * Recibe los datos ya leidos (los adjudicados vienen en una sola lista: una acta tiene como mucho unos
 * * cientos de filas, los cupos de la convocatoria) y no toca la base ni MinIO. La plantilla se compila una
 * * sola vez. Zona `America/La_Paz` y locale `es-BO`, como la lista priorizada (CU06).
 * * El numero «borrador» se imprime tal cual y el titulo lleva la marca «BORRADOR – sin valor oficial».
 * ! Un fallo de Jasper se loguea con la convocatoria y el tipo de error, nunca con nombres ni CI, y se relanza
 * ! como `IllegalStateException` (el handler global lo responde como `500` generico).
 */
@Service
public class ActaPdfService {

    private static final Logger LOG = LoggerFactory.getLogger(ActaPdfService.class);

    static final String PLANTILLA = "/reports/acta_adjudicacion.jrxml";
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final Locale LOCALE = Locale.forLanguageTag("es-BO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SIN_DATO = "—";

    private final String plantilla;

    private volatile JasperReport plantillaCompilada;

    /** Todo lo que lleva el acta, ya leido: la convocatoria, los adjudicados y quien la genera. */
    public record DatosActa(UUID convocatoriaCodigo, String convocatoria, String gestion, Instant fechaApertura,
            Instant fechaCierre, Integer cupos, BigDecimal puntajeMinimo, List<Adjudicado> adjudicados,
            String generadaPor, Instant generadaEn) {
    }

    public ActaPdfService() {
        this(PLANTILLA);
    }

    /** Con otra plantilla: solo para probar el fallo de la plantilla. */
    ActaPdfService(String plantilla) {
        this.plantilla = plantilla;
    }

    /** El PDF del acta con ese numero (`borrador` para la vista previa). */
    public byte[] generar(DatosActa datos, String numero) {
        try {
            boolean borrador = NumeroActaService.BORRADOR.equals(numero);
            Map<String, Object> parametros = new HashMap<>();
            parametros.put(JRParameter.REPORT_LOCALE, LOCALE);
            parametros.put("TITULO", borrador ? "Acta de adjudicación – BORRADOR – sin valor oficial"
                    : "Acta de adjudicación");
            parametros.put("NUMERO", numero);
            parametros.put("CONVOCATORIA", datos.convocatoria());
            parametros.put("GESTION", datos.gestion());
            parametros.put("APERTURA", fecha(datos.fechaApertura()));
            parametros.put("CIERRE", fecha(datos.fechaCierre()));
            parametros.put("CUPOS", datos.cupos() == null ? SIN_DATO : String.valueOf(datos.cupos()));
            parametros.put("MINIMO", puntaje(datos.puntajeMinimo()));
            parametros.put("GENERADO_EN", FECHA_HORA.format(datos.generadaEn().atZone(ZONA)));
            parametros.put("GENERADO_POR", datos.generadaPor());
            parametros.put("RESUMEN", "Adjudicados: " + datos.adjudicados().size() + " de "
                    + (datos.cupos() == null ? SIN_DATO : datos.cupos()) + " cupos");
            parametros.put("HAY_ADJUDICADOS", !datos.adjudicados().isEmpty());

            JasperPrint impreso = JasperFillManager.fillReport(plantilla(), parametros,
                    new JRMapCollectionDataSource(filas(datos.adjudicados())));

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            JRPdfExporter exportador = new JRPdfExporter();
            exportador.setExporterInput(new SimpleExporterInput(impreso));
            exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));
            exportador.exportReport();
            return salida.toByteArray();
        } catch (JRException | RuntimeException e) {
            LOG.error("No se pudo generar el PDF del acta de adjudicación", kv("module", "actas"),
                    kv("convocatoria", datos.convocatoriaCodigo()), kv("error", e.getClass().getSimpleName()));
            throw new IllegalStateException("No se pudo generar el PDF del acta de adjudicación", e);
        }
    }

    private static List<Map<String, ?>> filas(List<Adjudicado> adjudicados) {
        List<Map<String, ?>> filas = new ArrayList<>(adjudicados.size());
        int orden = 1;
        for (Adjudicado a : adjudicados) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("orden", String.valueOf(orden++));
            fila.put("nombre", a.nombres() + " " + a.apellidos());
            fila.put("ci", a.ci());
            fila.put("carrera", a.carrera());
            fila.put("puntaje", puntaje(a.puntajeFinal()));
            filas.add(fila);
        }
        return filas;
    }

    private static String puntaje(BigDecimal valor) {
        return valor == null ? SIN_DATO : String.format(LOCALE, "%.2f", valor);
    }

    private static String fecha(Instant instante) {
        return instante == null ? SIN_DATO : FECHA.format(instante.atZone(ZONA));
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
