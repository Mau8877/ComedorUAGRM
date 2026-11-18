package com.comedoruagrm.backend.common.report;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;

/**
 * Base compartida de los PDF del sistema (JasperReports): compila las plantillas una sola vez y exporta a PDF.
 *
 * * Todas las plantillas de `reports/` llevan la marca `<!--ESTILOS_UAGRM-->` donde van los estilos comunes
 * * (paleta UAGRM/DUBSS, cabecera, tabla con filas alternadas, tarjetas): se reemplaza al cargar por el contenido
 * * de `reports/_estilos_uagrm.xml`, asi el diseno se cambia en un solo archivo.
 */
public final class ReportePdf {

    static final String MARCA_ESTILOS = "<!--ESTILOS_UAGRM-->";
    static final String ESTILOS = "/reports/_estilos_uagrm.xml";

    private static final Map<String, JasperReport> CACHE = new ConcurrentHashMap<>();

    private ReportePdf() {
    }

    /** Compila (y cachea) la plantilla del classpath, con los estilos comunes ya insertados. */
    public static JasperReport compilar(String recurso) throws JRException {
        JasperReport compilada = CACHE.get(recurso);
        if (compilada == null) {
            try {
                String fuente = new String(leer(recurso), StandardCharsets.UTF_8)
                        .replace(MARCA_ESTILOS, new String(leer(ESTILOS), StandardCharsets.UTF_8));
                compilada = JasperCompileManager.compileReport(
                        new ByteArrayInputStream(fuente.getBytes(StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new JRException(e);
            }
            CACHE.put(recurso, compilada);
        }
        return compilada;
    }

    public static JasperPrint llenar(JasperReport plantilla, Map<String, Object> parametros, JRDataSource datos)
            throws JRException {
        return JasperFillManager.fillReport(plantilla, parametros, datos);
    }

    public static byte[] exportar(JasperPrint impreso) throws JRException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        JRPdfExporter exportador = new JRPdfExporter();
        exportador.setExporterInput(new SimpleExporterInput(impreso));
        exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));
        exportador.exportReport();
        return salida.toByteArray();
    }

    private static byte[] leer(String recurso) throws IOException, JRException {
        try (InputStream in = ReportePdf.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new JRException("No se encontró la plantilla " + recurso);
            }
            return in.readAllBytes();
        }
    }
}
