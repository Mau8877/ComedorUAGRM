package com.comedoruagrm.backend.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.comedoruagrm.backend.common.storage.AlmacenamientoException;
import com.comedoruagrm.backend.common.storage.AlmacenamientoService;

import lombok.RequiredArgsConstructor;

/**
 * Expedientes de ejemplo para ver y probar el CU07 (auditar el expediente de un finalista) en desarrollo.
 *
 * * Solo con el perfil `dev`, por defecto activo; se apaga con `app.seed.expedientes.enabled=false`.
 * * Corre despues de los cargadores del CU06 y CU21 (que crean la convocatoria `[PRUEBA]` y sus 10
 * * finalistas) y no hace nada si no los encuentra. Es idempotente: si ya hay respuestas socioeconomicas en
 * * la convocatoria, no hace nada.
 * * Deja a los 10 finalistas con su respuesta socioeconomica, sus documentos y situaciones variadas
 * * (validado, observado, no valido, con dos auditorias, sin auditar, con la entrega pendiente).
 * * Sube a MinIO un PDF pequeno por documento; si MinIO no esta disponible deja un WARN y guarda igual las
 * * filas, para poder probar el error al abrir un documento.
 * * No cambia el estado de ninguna postulacion ni el puntaje de la IA.
 * ! NUNCA en produccion.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.expedientes.enabled", havingValue = "true", matchIfMissing = true)
@Order(4)
@RequiredArgsConstructor
public class DatosPruebaExpedientes implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosPruebaExpedientes.class);

    static final String CARNET = "Carnet de identidad";
    static final String INGRESOS = "Certificado de ingresos";
    static final String NOTAS = "Certificado de notas";
    static final String LUZ = "Fotocopia de luz";

    private static final String CONVOCATORIA = DatosPruebaListaPriorizada.MARCA + " Beca Comedor 2026-II";
    private static final String[] PATRIMONIO = {"Vivienda alquilada", "Vivienda prestada", "Vivienda propia modesta"};
    private static final String[] PROCEDENCIA = {"Montero", "Warnes", "Cotoca", "Camiri", "Santa Cruz de la Sierra"};

    /** Un archivo por subir a MinIO despues de guardar las filas. */
    private record Subida(String ruta, byte[] contenido) {
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;
    private final AlmacenamientoService almacenamiento;

    @Override
    public void run(String... args) {
        List<Long> convocatorias = jdbc.queryForList(
                "SELECT id FROM convocatoria WHERE nombre = ? AND eliminado_en IS NULL", Long.class, CONVOCATORIA);
        List<Long> administradores = jdbc.queryForList("""
                SELECT u.id FROM usuario u JOIN rol r ON r.id = u.rol_id
                WHERE r.nombre = 'ADMINISTRADOR_DUBSS' AND u.eliminado_en IS NULL AND u.activo
                ORDER BY u.id LIMIT 1
                """, Long.class);
        if (convocatorias.isEmpty() || administradores.isEmpty()) {
            LOG.warn("No se cargaron los expedientes de prueba: falta la convocatoria de prueba o un "
                    + "Administrador DUBSS");
            return;
        }
        long convocatoria = convocatorias.get(0);
        Integer finalistas = jdbc.queryForObject("""
                SELECT count(*) FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.convocatoria_id = ? AND f.eliminado_en IS NULL
                """, Integer.class, convocatoria);
        if (finalistas == null || finalistas == 0) {
            LOG.warn("No se cargaron los expedientes de prueba: la convocatoria de prueba no tiene finalistas");
            return;
        }
        Integer cargados = jdbc.queryForObject("""
                SELECT count(*) FROM respuesta_socioeconomica r JOIN postulacion po ON po.id = r.postulacion_id
                WHERE po.convocatoria_id = ?
                """, Integer.class, convocatoria);
        if (cargados != null && cargados > 0) {
            return;
        }
        List<Subida> subidas = new ArrayList<>();
        transacciones.executeWithoutResult(
                estado -> subidas.addAll(cargar(convocatoria, administradores.get(0))));
        subir(subidas);
        LOG.info("Expedientes de prueba cargados para {} finalistas", finalistas);
    }

    private List<Subida> cargar(long convocatoria, long auditor) {
        long carnet = tipo(CARNET);
        long ingresos = tipo(INGRESOS);
        long notas = tipo(NOTAS);
        long luz = tipo(LUZ);
        exigir(convocatoria, carnet, true);
        exigir(convocatoria, ingresos, true);
        exigir(convocatoria, notas, true);
        exigir(convocatoria, luz, false);

        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT f.id AS finalista, f.posicion, po.id AS postulacion, po.postulante_id AS postulante
                FROM finalista f JOIN postulacion po ON po.id = f.postulacion_id
                WHERE po.convocatoria_id = ? AND f.eliminado_en IS NULL AND po.eliminado_en IS NULL
                ORDER BY f.posicion
                """, convocatoria);
        Map<Long, String> tipos = Map.of(carnet, CARNET, ingresos, INGRESOS, notas, NOTAS, luz, LUZ);
        List<Subida> subidas = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            long finalista = ((Number) fila.get("finalista")).longValue();
            int posicion = ((Number) fila.get("posicion")).intValue();
            long postulacion = ((Number) fila.get("postulacion")).longValue();
            long postulante = ((Number) fila.get("postulante")).longValue();

            respuesta(postulacion, posicion);
            for (long tipo : documentosDe(posicion, carnet, ingresos, notas, luz)) {
                subidas.add(documento(postulacion, tipo, tipos.get(tipo)));
            }
            // La entrega fisica: del 1 al 5 ya llego (el 1 la trae del cargador del CU06).
            if (posicion >= 2 && posicion <= 5) {
                jdbc.update("""
                        UPDATE finalista SET documentacion_recibida_en = now() - interval '1 day'
                        WHERE id = ? AND documentacion_recibida_en IS NULL""", finalista);
            }
            auditorias(finalista, posicion, auditor);
            if (posicion == 2) {
                avisoObservado(postulacion, postulante);
            }
        }
        return subidas;
    }

    /** Documentos adjuntos segun la situacion: el 1, 3, 4 y 5 con los 4, el 2 sin notas, el resto sin la luz. */
    private static long[] documentosDe(int posicion, long carnet, long ingresos, long notas, long luz) {
        return switch (posicion) {
            case 1, 3, 4, 5 -> new long[] {carnet, ingresos, notas, luz};
            case 2 -> new long[] {carnet, ingresos, luz};
            default -> new long[] {carnet, ingresos, notas};
        };
    }

    private void auditorias(long finalista, int posicion, long auditor) {
        switch (posicion) {
            case 1 -> auditoria(finalista, auditor, "VALIDADO", null, "25.00", 12);
            case 2 -> auditoria(finalista, auditor, "OBSERVADO", "Falta el certificado de notas.", null, 24);
            case 4 -> {
                auditoria(finalista, auditor, "OBSERVADO", "La fotocopia de luz no se lee.", null, 72);
                auditoria(finalista, auditor, "VALIDADO", null, "22.50", 24);
            }
            case 5 -> auditoria(finalista, auditor, "NO_VALIDO",
                    "El carnet de identidad no coincide con el del postulante.", null, 24);
            default -> {
                // Sin auditoria: el 3 ya entrego y el resto tiene la entrega pendiente.
            }
        }
    }

    private void auditoria(long finalista, long auditor, String resultado, String observaciones,
            String puntajeAcademico, int horasAtras) {
        jdbc.update("""
                INSERT INTO auditoria_expediente (finalista_id, auditor_id, resultado, observaciones,
                                                  puntaje_academico, fecha_auditoria)
                VALUES (?, ?, CAST(? AS resultado_auditoria), ?, CAST(? AS numeric),
                        now() - (? * interval '1 hour'))
                """, finalista, auditor, resultado, observaciones, puntajeAcademico, horasAtras);
    }

    private void avisoObservado(long postulacion, long postulante) {
        jdbc.update("""
                INSERT INTO notificacion (usuario_id, postulacion_id, titulo, mensaje, tipo, leida, enviada_en)
                VALUES (?, ?, 'Tu expediente fue observado - Beca Comedor',
                        'Hola, la DUBSS observó tu expediente. Observaciones: Falta el certificado de notas.',
                        CAST('WARNING' AS tipo_notificacion), false, now() - interval '1 day')
                """, postulante, postulacion);
    }

    private void respuesta(long postulacion, int posicion) {
        jdbc.update("""
                INSERT INTO respuesta_socioeconomica (postulacion_id, ingresos_familiares, dependientes, patrimonio,
                                                      procedencia, promedio_academico, estado_vulnerabilidad)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, postulacion, 800 + posicion * 150, 2 + posicion % 4, PATRIMONIO[posicion % PATRIMONIO.length],
                PROCEDENCIA[posicion % PROCEDENCIA.length], 60 + posicion * 2.5, posicion <= 5 ? "ALTA" : "MEDIA");
    }

    private Subida documento(long postulacion, long tipo, String nombreTipo) {
        String archivo = nombreTipo.toLowerCase(Locale.ROOT).replace(' ', '-') + ".pdf";
        String ruta = "prueba/postulaciones/" + postulacion + "/" + archivo;
        byte[] contenido = pdf(nombreTipo + " - documento de prueba");
        jdbc.update("""
                INSERT INTO documento_adjunto (postulacion_id, tipo_documento_id, nombre_archivo, mime_type,
                                               storage_path, tamano_bytes)
                VALUES (?, ?, ?, 'application/pdf', ?, ?)
                """, postulacion, tipo, archivo, ruta, contenido.length);
        return new Subida(ruta, contenido);
    }

    private long tipo(String nombre) {
        List<Long> existente = jdbc.queryForList(
                "SELECT id FROM tipo_documento WHERE nombre = ? AND eliminado_en IS NULL", Long.class, nombre);
        if (!existente.isEmpty()) {
            return existente.get(0);
        }
        return jdbc.queryForObject("INSERT INTO tipo_documento (nombre) VALUES (?) RETURNING id", Long.class, nombre);
    }

    private void exigir(long convocatoria, long tipo, boolean obligatorio) {
        jdbc.update("""
                INSERT INTO convocatoria_documento (convocatoria_id, tipo_documento_id, obligatorio)
                VALUES (?, ?, ?)
                """, convocatoria, tipo, obligatorio);
    }

    /** Sube los PDF a MinIO; al primer fallo avisa una sola vez y deja las filas igual. */
    private void subir(List<Subida> subidas) {
        for (Subida subida : subidas) {
            try {
                almacenamiento.subir(subida.ruta(), subida.contenido(), "application/pdf");
            } catch (AlmacenamientoException e) {
                LOG.warn("MinIO no está disponible: los documentos de prueba quedan registrados pero sus "
                        + "archivos no se subieron");
                return;
            }
        }
    }

    /** Un PDF de una pagina con el texto dado, armado en memoria (solo ASCII). */
    static byte[] pdf(String texto) {
        String limpio = texto.replaceAll("[^\\x20-\\x7E]", "").replaceAll("[()\\\\]", "");
        String contenido = "BT /F1 18 Tf 72 720 Td (" + limpio + ") Tj ET";
        List<String> objetos = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R "
                        + "/Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + contenido.length() + " >>\nstream\n" + contenido + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> posiciones = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            posiciones.add(pdf.length());
            pdf.append(i + 1).append(" 0 obj\n").append(objetos.get(i)).append("\nendobj\n");
        }
        int inicioXref = pdf.length();
        pdf.append("xref\n0 ").append(objetos.size() + 1).append("\n0000000000 65535 f \n");
        for (int posicion : posiciones) {
            pdf.append(String.format("%010d 00000 n \n", posicion));
        }
        pdf.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(inicioXref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
