package com.comedoruagrm.backend.features.dashboard.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.AsistenciaDia;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.Carrera;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.ConvocatoriaComparada;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.Facultad;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.GestionTendencia;
import com.comedoruagrm.backend.features.dashboard.dto.BiResponse.TramoPuntaje;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse;
import com.comedoruagrm.backend.features.dashboard.dto.IndicadoresResponse.IndicadorMensual;
import com.comedoruagrm.backend.features.dashboard.exception.DashboardErrorCodes;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;

/**
 * Descarga del dashboard BI en Excel (.xlsx, Apache POI) (CU12).
 *
 * * Usa las mismas consultas que la pantalla (`IndicadoresService` y `BiService`), asi que el libro trae lo que ve
 * * el Directivo: una hoja por bloque (resumen, mes a mes, facultades, carreras, convocatorias, puntajes,
 * * asistencia y tendencia). Sin datos no hay archivo (`ERR_DAS_02`).
 * * Solo lee y no se anota en la bitacora; son agregados sin datos personales. Quien lo genera sale del
 * * `actorCodigo` (el token).
 * ! Un fallo al escribir el libro se loguea sin datos personales y sube como error no controlado (500).
 */
@Service
@RequiredArgsConstructor
public class BiExcelService {

    private static final Logger LOG = LoggerFactory.getLogger(BiExcelService.class);

    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private final IndicadoresService indicadores;
    private final BiService bi;
    private final UsuarioService usuarioService;
    private final Clock reloj;

    /** El libro ya generado y el nombre con el que se descarga. */
    public record ExcelGenerado(String nombreArchivo, byte[] contenido) {
    }

    @PreAuthorize("hasRole('DIRECTIVO')")
    @Transactional(readOnly = true)
    public ExcelGenerado generar(UUID actorCodigo, UUID gestion, UUID convocatoria) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        IndicadoresResponse datos = indicadores.obtener(gestion, convocatoria);
        BiResponse analisis = bi.obtener(gestion, convocatoria);
        if (datos.sinDatos()) {
            throw new ValidationException(DashboardErrorCodes.ERR_DAS_02,
                    "No hay datos que exportar para esa selección.");
        }
        Instant ahora = reloj.instant();
        return new ExcelGenerado(nombreArchivo(datos.gestion().etiqueta(), ahora),
                escribir(datos, analisis, actor, ahora));
    }

    private byte[] escribir(IndicadoresResponse datos, BiResponse analisis, Usuario actor, Instant ahora) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle titulo = estiloEncabezado(libro);
            hojaResumen(libro, titulo, datos, analisis, actor, ahora);
            hojaMeses(libro, titulo, datos.porMes());
            hojaFacultades(libro, titulo, analisis.facultades());
            hojaCarreras(libro, titulo, analisis.facultades());
            hojaConvocatorias(libro, titulo, analisis.convocatorias());
            hojaPuntaje(libro, titulo, analisis.puntaje());
            hojaAsistencia(libro, titulo, analisis.asistencia().porDiaSemana());
            hojaTendencia(libro, titulo, analisis.tendencia());
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            LOG.error("No se pudo generar el Excel del dashboard BI", e);
            throw new IllegalStateException("No se pudo generar el Excel del dashboard", e);
        }
    }

    private void hojaResumen(XSSFWorkbook libro, CellStyle titulo, IndicadoresResponse datos, BiResponse analisis,
            Usuario actor, Instant ahora) {
        Sheet hoja = libro.createSheet("Resumen");
        int fila = 0;
        fila = par(hoja, fila, "Gestión académica", datos.gestion().etiqueta());
        fila = par(hoja, fila, "Convocatoria",
                datos.convocatoria() == null ? "Todas las convocatorias" : datos.convocatoria().nombre());
        fila = par(hoja, fila, "Datos actualizados",
                datos.actualizadoEn() == null ? "Aún no calculado" : FECHA_HORA.format(datos.actualizadoEn().atZone(ZONA)));
        fila = par(hoja, fila, "Generado el", FECHA_HORA.format(ahora.atZone(ZONA)));
        fila = par(hoja, fila, "Generado por", actor.getNombres() + " " + actor.getApellidos());
        fila++;
        encabezado(hoja, fila++, titulo, "Indicador", "Valor");
        fila = par(hoja, fila, "Postulaciones recibidas", analisis.embudo().postulaciones());
        fila = par(hoja, fila, "Precalificadas con IA", analisis.embudo().precalificadas());
        fila = par(hoja, fila, "Finalistas", analisis.embudo().finalistas());
        fila = par(hoja, fila, "Becas adjudicadas", analisis.embudo().adjudicadas());
        fila = par(hoja, fila, "Becas vigentes", analisis.embudo().becasVigentes());
        fila = par(hoja, fila, "Cupos", analisis.cobertura().cupos());
        fila = par(hoja, fila, "Cobertura de cupos (%)", analisis.cobertura().porcentaje());
        fila = par(hoja, fila, "Puntaje promedio de la IA", analisis.puntajePromedio());
        fila = par(hoja, fila, "Asistencia promedio (%)", datos.totales().asistenciaPromedio());
        fila = par(hoja, fila, "Becas en riesgo de asistencia", analisis.asistencia().becasEnRiesgo());
        ajustar(hoja, 2);
    }

    private void hojaMeses(XSSFWorkbook libro, CellStyle titulo, List<IndicadorMensual> meses) {
        Sheet hoja = libro.createSheet("Por mes");
        encabezado(hoja, 0, titulo, "Mes", "Postulaciones", "Finalistas", "Adjudicadas", "Asistencia (%)");
        int fila = 1;
        for (IndicadorMensual m : meses) {
            fila = par(hoja, fila, m.mes(), m.postulaciones(), m.finalistas(), m.adjudicadas(), m.asistencia());
        }
        ajustar(hoja, 5);
    }

    private void hojaFacultades(XSSFWorkbook libro, CellStyle titulo, List<Facultad> facultades) {
        Sheet hoja = libro.createSheet("Por facultad");
        encabezado(hoja, 0, titulo, "Facultad", "Postulaciones", "% del total", "Adjudicadas", "Tasa de adjudicación (%)");
        int fila = 1;
        for (Facultad f : facultades) {
            fila = par(hoja, fila, f.nombre(), f.postulaciones(), f.porcentajePostulaciones(), f.adjudicadas(),
                    f.tasaAdjudicacion());
        }
        ajustar(hoja, 5);
    }

    private void hojaCarreras(XSSFWorkbook libro, CellStyle titulo, List<Facultad> facultades) {
        Sheet hoja = libro.createSheet("Por carrera");
        encabezado(hoja, 0, titulo, "Facultad", "Carrera", "Postulaciones", "Adjudicadas");
        int fila = 1;
        for (Facultad f : facultades) {
            for (Carrera c : f.carreras()) {
                fila = par(hoja, fila, f.nombre(), c.nombre(), c.postulaciones(), c.adjudicadas());
            }
        }
        ajustar(hoja, 4);
    }

    private void hojaConvocatorias(XSSFWorkbook libro, CellStyle titulo, List<ConvocatoriaComparada> convocatorias) {
        Sheet hoja = libro.createSheet("Convocatorias");
        encabezado(hoja, 0, titulo, "Convocatoria", "Cupos", "Postulaciones", "Variación vs anterior (%)",
                "Finalistas", "Adjudicadas", "Tasa de adjudicación (%)", "Puntaje promedio");
        int fila = 1;
        for (ConvocatoriaComparada c : convocatorias) {
            fila = par(hoja, fila, c.nombre(), c.cupos(), c.postulaciones(), c.variacionPostulaciones(),
                    c.finalistas(), c.adjudicadas(), c.tasaAdjudicacion(), c.puntajePromedio());
        }
        ajustar(hoja, 8);
    }

    private void hojaPuntaje(XSSFWorkbook libro, CellStyle titulo, List<TramoPuntaje> tramos) {
        Sheet hoja = libro.createSheet("Puntajes");
        encabezado(hoja, 0, titulo, "Tramo de puntaje (IA, 0 a 70)", "Postulaciones");
        int fila = 1;
        for (TramoPuntaje t : tramos) {
            fila = par(hoja, fila, t.desde() + " a " + t.hasta(), t.cantidad());
        }
        ajustar(hoja, 2);
    }

    private void hojaAsistencia(XSSFWorkbook libro, CellStyle titulo, List<AsistenciaDia> dias) {
        Sheet hoja = libro.createSheet("Asistencia");
        encabezado(hoja, 0, titulo, "Día de la semana", "Asistencia (%)");
        int fila = 1;
        for (AsistenciaDia d : dias) {
            fila = par(hoja, fila, DIAS[d.dia() - 1], d.tasa());
        }
        ajustar(hoja, 2);
    }

    private void hojaTendencia(XSSFWorkbook libro, CellStyle titulo, List<GestionTendencia> gestiones) {
        Sheet hoja = libro.createSheet("Tendencia");
        encabezado(hoja, 0, titulo, "Gestión", "Postulaciones", "Variación vs anterior (%)", "Adjudicadas");
        int fila = 1;
        for (GestionTendencia g : gestiones) {
            fila = par(hoja, fila, g.etiqueta(), g.postulaciones(), g.variacionPostulaciones(), g.adjudicadas());
        }
        ajustar(hoja, 4);
    }

    private static CellStyle estiloEncabezado(XSSFWorkbook libro) {
        Font negrita = libro.createFont();
        negrita.setBold(true);
        negrita.setColor(IndexedColors.WHITE.getIndex());
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(negrita);
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private static void encabezado(Sheet hoja, int fila, CellStyle estilo, String... titulos) {
        Row row = hoja.createRow(fila);
        for (int i = 0; i < titulos.length; i++) {
            Cell celda = row.createCell(i);
            celda.setCellValue(titulos[i]);
            celda.setCellStyle(estilo);
        }
    }

    /** Escribe una fila: texto, numero o vacio (un valor null queda en blanco, no en cero). Devuelve la siguiente. */
    private static int par(Sheet hoja, int fila, Object... valores) {
        Row row = hoja.createRow(fila);
        for (int i = 0; i < valores.length; i++) {
            Object v = valores[i];
            if (v == null) {
                continue;
            }
            Cell celda = row.createCell(i);
            if (v instanceof Number n) {
                celda.setCellValue(n.doubleValue());
            } else {
                celda.setCellValue(v.toString());
            }
        }
        return fila + 1;
    }

    private static void ajustar(Sheet hoja, int columnas) {
        for (int i = 0; i < columnas; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    /** `bi-{etiqueta-de-gestion-sin-espacios}-{yyyyMMdd}.xlsx`. */
    static String nombreArchivo(String etiquetaGestion, Instant ahora) {
        String limpio = Normalizer.normalize(etiquetaGestion, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return "bi-" + (limpio.isEmpty() ? "gestion" : limpio) + "-" + FECHA_ARCHIVO.format(ahora.atZone(ZONA))
                + ".xlsx";
    }
}
