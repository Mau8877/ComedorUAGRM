package com.comedoruagrm.backend.features.dashboard.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.IndicadorMensual;
import com.comedoruagrm.backend.features.dashboard.exception.DashboardErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;
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
 * Descarga del dashboard de indicadores en PDF con JasperReports (CU12).
 *
 * * Usa la misma consulta que la pantalla, asi que el PDF muestra exactamente lo que ve el Directivo. Sin datos
 * * no hay PDF (`ERR_DAS_02`). La plantilla se compila una sola vez.
 * * Solo lee y no se anota en la bitacora: son agregados sin datos personales. Quien lo genera sale del
 * * `actorCodigo` (el token), nunca de un parametro libre.
 * ! Un fallo al generar se loguea sin datos personales y sube como error no controlado (500).
 */
@Service
@RequiredArgsConstructor
public class IndicadoresPdfService {

    private static final Logger LOG = LoggerFactory.getLogger(IndicadoresPdfService.class);

    static final String PLANTILLA = "/reports/indicadores_dashboard.jrxml";
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SIN_DATOS = "Sin datos";

    private final IndicadoresService indicadores;
    private final UsuarioService usuarioService;
    private final Clock reloj;

    private volatile JasperReport plantillaCompilada;

    /** El PDF ya generado y el nombre con el que se descarga. */
    public record PdfGenerado(String nombreArchivo, byte[] contenido) {
    }

    @PreAuthorize("hasRole('DIRECTIVO')")
    @Transactional(readOnly = true)
    public PdfGenerado generar(UUID actorCodigo, UUID gestion, UUID convocatoria) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        IndicadoresResponse datos = indicadores.obtener(gestion, convocatoria);
        if (datos.sinDatos()) {
            throw new ValidationException(DashboardErrorCodes.ERR_DAS_02,
                    "No hay datos que exportar para esa selección.");
        }
        Instant ahora = reloj.instant();
        byte[] pdf = renderizar(datos, actor, ahora);
        return new PdfGenerado(nombreArchivo(datos.gestion().etiqueta(), ahora), pdf);
    }

    private byte[] renderizar(IndicadoresResponse datos, Usuario actor, Instant ahora) {
        try {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put(JRParameter.REPORT_LOCALE, Locale.forLanguageTag("es-BO"));
            parametros.put("GESTION", datos.gestion().etiqueta());
            parametros.put("CONVOCATORIA",
                    datos.convocatoria() == null ? "Todas las convocatorias" : datos.convocatoria().nombre());
            parametros.put("ACTUALIZADO_EN", datos.actualizadoEn() == null
                    ? "Aún no calculado" : FECHA_HORA.format(datos.actualizadoEn().atZone(ZONA)));
            parametros.put("TOTAL_POSTULACIONES", String.valueOf(datos.totales().postulacionesRecibidas()));
            parametros.put("TOTAL_FINALISTAS", String.valueOf(datos.totales().finalistas()));
            parametros.put("TOTAL_ADJUDICADAS", String.valueOf(datos.totales().becasAdjudicadas()));
            parametros.put("TOTAL_ASISTENCIA", texto(datos.totales().asistenciaPromedio()));
            parametros.put("GENERADO_EN", FECHA_HORA.format(ahora.atZone(ZONA)));
            parametros.put("GENERADO_POR", actor.getNombres() + " " + actor.getApellidos());

            JasperPrint impreso = JasperFillManager.fillReport(plantilla(), parametros,
                    new JRMapCollectionDataSource(filas(datos.porMes())));
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            JRPdfExporter exportador = new JRPdfExporter();
            exportador.setExporterInput(new SimpleExporterInput(impreso));
            exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));
            exportador.exportReport();
            return salida.toByteArray();
        } catch (JRException e) {
            LOG.error("No se pudo generar el PDF del dashboard de indicadores", e);
            throw new IllegalStateException("No se pudo generar el PDF del dashboard", e);
        }
    }

    private static List<Map<String, ?>> filas(List<IndicadorMensual> porMes) {
        List<Map<String, ?>> filas = new ArrayList<>();
        for (IndicadorMensual m : porMes) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("mes", m.mes());
            fila.put("postulaciones", String.valueOf(m.postulaciones()));
            fila.put("finalistas", String.valueOf(m.finalistas()));
            fila.put("adjudicadas", String.valueOf(m.adjudicadas()));
            fila.put("asistencia", texto(m.asistencia()));
            filas.add(fila);
        }
        return filas;
    }

    // TODO CU-10: mientras no exista el registro de asistencia, el valor es null y se muestra «Sin datos».
    private static String texto(Double asistencia) {
        return asistencia == null ? SIN_DATOS : String.format(Locale.forLanguageTag("es-BO"), "%.1f %%", asistencia);
    }

    // La plantilla se compila una vez y se reutiliza.
    private JasperReport plantilla() throws JRException {
        JasperReport compilada = plantillaCompilada;
        if (compilada == null) {
            try (InputStream fuente = getClass().getResourceAsStream(PLANTILLA)) {
                if (fuente == null) {
                    throw new JRException("No se encontró la plantilla " + PLANTILLA);
                }
                compilada = JasperCompileManager.compileReport(fuente);
            } catch (IOException e) {
                throw new JRException(e);
            }
            plantillaCompilada = compilada;
        }
        return compilada;
    }

    /** `indicadores-{etiqueta-de-gestion-sin-espacios}-{yyyyMMdd}.pdf`. */
    static String nombreArchivo(String etiquetaGestion, Instant ahora) {
        String limpio = Normalizer.normalize(etiquetaGestion, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return "indicadores-" + (limpio.isEmpty() ? "gestion" : limpio) + "-"
                + FECHA_ARCHIVO.format(ahora.atZone(ZONA)) + ".pdf";
    }
}
