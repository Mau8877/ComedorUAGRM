package com.comedoruagrm.backend.features.postulaciones.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comedoruagrm.backend.common.auditoria.Auditar;
import com.comedoruagrm.backend.common.auditoria.ResultadoAuditado;
import com.comedoruagrm.backend.common.exception.NotFoundException;
import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.bitacora.constant.BitacoraAcciones;
import com.comedoruagrm.backend.features.postulaciones.dto.ExportarListaRequest;
import com.comedoruagrm.backend.features.postulaciones.exception.PriorizacionErrorCodes;
import com.comedoruagrm.backend.features.postulaciones.repository.ListaPriorizadaRepository;
import com.comedoruagrm.backend.features.postulaciones.repository.ListaPriorizadaRepository.DatosConvocatoria;
import com.comedoruagrm.backend.features.usuarios.model.Usuario;
import com.comedoruagrm.backend.features.usuarios.service.UsuarioService;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.fill.JRFileVirtualizer;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;

/**
 * Exporta la lista priorizada a PDF con JasperReports (CU06, paso 7).
 *
 * * Exporta la lista COMPLETA de la convocatoria con la busqueda y el filtro de carrera vigentes,
 * * en el mismo orden y con la misma posicion que la pantalla. Lee por tramos y usa un virtualizador
 * * para no tener las 20 000 filas en memoria. La plantilla se compila una sola vez.
 * * La exportacion se anota en la bitacora (saca datos personales del sistema): `detalle` lleva la
 * * convocatoria, la carrera, si hubo busqueda y la cantidad de filas.
 * ! Nunca nombres, CI ni el texto buscado en la bitacora ni en los logs (podria ser un CI).
 * ! No es `readOnly`: la anotacion de la bitacora escribe en la misma transaccion.
 */
@Service
@RequiredArgsConstructor
public class ListaPriorizadaPdfService {

    private static final Logger LOG = LoggerFactory.getLogger(ListaPriorizadaPdfService.class);

    static final String PLANTILLA = "/reports/lista_priorizada.jrxml";
    static final int TRAMO = 1000;
    private static final int PAGINAS_EN_MEMORIA = 20;
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ListaPriorizadaRepository repository;
    private final UsuarioService usuarioService;

    private volatile JasperReport plantillaCompilada;

    /** El PDF ya generado y el nombre con el que se descarga. */
    public record PdfGenerado(String nombreArchivo, byte[] contenido) {
    }

    @PreAuthorize("hasRole('ADMINISTRADOR_DUBSS')")
    @Transactional
    @Auditar(accion = BitacoraAcciones.EXPORTACION_LISTA_PRIORIZADA, entidad = "convocatoria")
    public ResultadoAuditado<PdfGenerado> exportar(UUID actorCodigo, ExportarListaRequest request) {
        Usuario actor = usuarioService.obtenerPorCodigo(actorCodigo);
        DatosConvocatoria convocatoria = repository.buscarConvocatoria(request.convocatoriaId())
                .orElseThrow(() -> new NotFoundException(PriorizacionErrorCodes.ERR_PRI_01,
                        "La convocatoria no existe"));

        String busqueda = ListaPriorizadaService.patronDeBusqueda(request.search());
        long total = repository.contar(request.convocatoriaId(), request.carreraId(), busqueda);
        if (total == 0) {
            throw new ValidationException(PriorizacionErrorCodes.ERR_PRI_03,
                    "No hay postulaciones para exportar con esos filtros");
        }

        Instant ahora = Instant.now();
        byte[] pdf = generar(convocatoria, request, busqueda, total, actor, ahora);
        LOG.info("Lista priorizada exportada a PDF: convocatoria={}, filas={}, userId={}",
                request.convocatoriaId(), total, actorCodigo);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("convocatoria", request.convocatoriaId().toString());
        detalle.put("carreraId", request.carreraId() == null ? null : request.carreraId().toString());
        detalle.put("conBusqueda", busqueda != null);
        detalle.put("filas", total);
        PdfGenerado resultado = new PdfGenerado(nombreArchivo(convocatoria.nombre(), ahora), pdf);
        return new ResultadoAuditado<>(resultado, actor.getId(), actor.getCodigo(), convocatoria.id(),
                detalle, true);
    }

    private byte[] generar(DatosConvocatoria convocatoria, ExportarListaRequest request, String busqueda,
            long total, Usuario actor, Instant ahora) {
        Path temporal = null;
        JRFileVirtualizer virtualizador = null;
        try {
            temporal = Files.createTempDirectory("lista-priorizada-");
            virtualizador = new JRFileVirtualizer(PAGINAS_EN_MEMORIA, temporal.toString());

            Map<String, Object> parametros = new HashMap<>();
            parametros.put(JRParameter.REPORT_VIRTUALIZER, virtualizador);
            parametros.put(JRParameter.REPORT_LOCALE, Locale.forLanguageTag("es-BO"));
            parametros.put("CONVOCATORIA", convocatoria.nombre());
            parametros.put("GESTION", convocatoria.gestion());
            parametros.put("CARRERA", request.carreraId() == null
                    ? "Todas" : repository.buscarNombreCarrera(request.carreraId()).orElse("Todas"));
            parametros.put("BUSQUEDA", busqueda == null ? "—" : request.search().trim());
            parametros.put("TOTAL", String.valueOf(total));
            parametros.put("GENERADO_EN", FECHA_HORA.format(ahora.atZone(ZONA)));
            parametros.put("GENERADO_POR", actor.getNombres() + " " + actor.getApellidos());

            ListaPriorizadaDataSource datos = new ListaPriorizadaDataSource(
                    desplazamiento -> repository.buscar(request.convocatoriaId(), request.carreraId(), busqueda,
                            TRAMO, desplazamiento), ZONA);
            JasperPrint impreso = JasperFillManager.fillReport(plantilla(), parametros, datos);

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            JRPdfExporter exportador = new JRPdfExporter();
            exportador.setExporterInput(new SimpleExporterInput(impreso));
            exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));
            exportador.exportReport();
            return salida.toByteArray();
        } catch (JRException e) {
            LOG.error("No se pudo generar el PDF de la lista priorizada: convocatoria={}",
                    request.convocatoriaId(), e);
            throw new IllegalStateException("No se pudo generar el PDF de la lista priorizada", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (virtualizador != null) {
                virtualizador.cleanup();
            }
            borrar(temporal);
        }
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

    private void borrar(Path directorio) {
        if (directorio == null) {
            return;
        }
        try (Stream<Path> archivos = Files.walk(directorio)) {
            archivos.sorted(java.util.Comparator.reverseOrder()).forEach(ruta -> ruta.toFile().delete());
        } catch (IOException e) {
            LOG.warn("No se pudieron borrar los archivos temporales de la exportación");
        }
    }

    /** `lista-priorizada-{convocatoria-en-minusculas-sin-tildes}-{yyyyMMdd}.pdf`. */
    static String nombreArchivo(String convocatoria, Instant ahora) {
        String limpio = Normalizer.normalize(convocatoria, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return "lista-priorizada-" + (limpio.isEmpty() ? "convocatoria" : limpio) + "-"
                + FECHA_ARCHIVO.format(ahora.atZone(ZONA)) + ".pdf";
    }
}
