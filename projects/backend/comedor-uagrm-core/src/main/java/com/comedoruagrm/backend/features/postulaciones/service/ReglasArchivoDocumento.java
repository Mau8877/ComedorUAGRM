package com.comedoruagrm.backend.features.postulaciones.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.comedoruagrm.backend.common.exception.ValidationException;
import com.comedoruagrm.backend.features.postulaciones.exception.DocumentoPostulacionErrorCodes;

/**
 * Reglas de formato y tamaño de un archivo de la postulacion (CU16): formatos permitidos, tamaño maximo y
 * cantidad maxima por tipo de documento. Los valores salen de `app.documentos.*` (por definir con la DUBSS).
 *
 * * El formato se decide por el CONTENIDO (los primeros bytes), no por la extension ni por el tipo que
 * * declara el cliente: un `.pdf` que en realidad es otra cosa se rechaza. Se reconocen PDF, JPEG y PNG.
 * * Si el cliente declara un tipo concreto (PDF, JPEG o PNG) y no coincide con el contenido, se rechaza;
 * * un tipo generico (`application/octet-stream`, vacio) no estorba.
 * * Funcion sin estado: no toca la red ni el almacenamiento.
 */
@Component
public class ReglasArchivoDocumento {

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final Set<String> CONOCIDOS = Set.of("application/pdf", "image/jpeg", "image/png");

    private final long tamanoMaximoBytes;
    private final Set<String> formatosPermitidos;
    private final int maximoArchivosPorTipo;

    public ReglasArchivoDocumento(@Value("${app.documentos.tamano-maximo-bytes}") long tamanoMaximoBytes,
            @Value("${app.documentos.formatos-permitidos}") Set<String> formatosPermitidos,
            @Value("${app.documentos.maximo-archivos-por-tipo}") int maximoArchivosPorTipo) {
        this.tamanoMaximoBytes = tamanoMaximoBytes;
        this.formatosPermitidos = Set.copyOf(formatosPermitidos);
        this.maximoArchivosPorTipo = maximoArchivosPorTipo;
    }

    /** El formato real de un archivo ya validado. `extension` va sin punto. */
    public record Formato(String mime, String extension) {
    }

    public long tamanoMaximoBytes() {
        return tamanoMaximoBytes;
    }

    public Set<String> formatosPermitidos() {
        return formatosPermitidos;
    }

    public int maximoArchivosPorTipo() {
        return maximoArchivosPorTipo;
    }

    /**
     * Devuelve el formato real del archivo o lanza el motivo del rechazo.
     *
     * @throws ValidationException `ERR_DOC_01` (vacio), `ERR_DOC_02` (muy grande) o `ERR_DOC_03` (formato)
     */
    public Formato validar(ArchivoSubido archivo) {
        byte[] contenido = archivo == null ? null : archivo.contenido();
        if (contenido == null || contenido.length == 0) {
            throw new ValidationException(DocumentoPostulacionErrorCodes.ERR_DOC_01, "No se recibió ningún archivo");
        }
        if (contenido.length > tamanoMaximoBytes) {
            throw new ValidationException(DocumentoPostulacionErrorCodes.ERR_DOC_02,
                    "El archivo supera el tamaño máximo de " + megas(tamanoMaximoBytes));
        }
        Formato real = detectar(contenido);
        if (real == null || !formatosPermitidos.contains(real.mime())) {
            throw new ValidationException(DocumentoPostulacionErrorCodes.ERR_DOC_03,
                    "El formato del archivo no está permitido. Se aceptan: " + nombresDeFormatos());
        }
        String declarado = normalizar(archivo.contentType());
        if (CONOCIDOS.contains(declarado) && !declarado.equals(real.mime())) {
            throw new ValidationException(DocumentoPostulacionErrorCodes.ERR_DOC_03,
                    "El contenido del archivo no corresponde a su formato");
        }
        return real;
    }

    private static Formato detectar(byte[] c) {
        if (empiezaCon(c, PDF)) {
            return new Formato("application/pdf", "pdf");
        }
        if (empiezaCon(c, JPEG)) {
            return new Formato("image/jpeg", "jpg");
        }
        if (empiezaCon(c, PNG)) {
            return new Formato("image/png", "png");
        }
        return null;
    }

    private static boolean empiezaCon(byte[] contenido, byte[] firma) {
        if (contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (contenido[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }

    /** `Image/JPG; charset=x` → `image/jpeg`. */
    private static String normalizar(String contentType) {
        if (contentType == null) {
            return "";
        }
        String t = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(t) ? "image/jpeg" : t;
    }

    private String nombresDeFormatos() {
        return formatosPermitidos.stream().sorted().map(f -> switch (f) {
            case "application/pdf" -> "PDF";
            case "image/jpeg" -> "JPG";
            case "image/png" -> "PNG";
            default -> f;
        }).reduce((a, b) -> a + ", " + b).orElse("ninguno");
    }

    private static String megas(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        return mb == Math.floor(mb) ? (long) mb + " MB" : String.format(Locale.ROOT, "%.1f MB", mb);
    }
}
