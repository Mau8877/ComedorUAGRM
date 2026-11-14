package com.comedoruagrm.backend.common.storage;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;

/**
 * Almacenamiento de archivos en MinIO (transversal: lo usan el expediente, CU07, y la subida de documentos, CU16).
 *
 * * La base solo guarda la ruta de cada objeto; los binarios viven aca.
 * * Un archivo se abre con una URL temporal firmada con el host publico, nunca con una ruta permanente.
 * * Un fallo de MinIO se loguea como ERROR (solo la operacion y el tipo de error: nunca la ruta ni la URL) y
 * * sube como {@link AlmacenamientoException}, que el handler global responde como 500 generico.
 * ! Constructor escrito a mano (no Lombok) porque lleva un @Qualifier entre los dos clientes de MinIO.
 */
@Service
public class AlmacenamientoService {

    private static final Logger LOG = LoggerFactory.getLogger(AlmacenamientoService.class);

    /** Cuanto dura la direccion con la que el administrador abre un documento. */
    public static final Duration VIGENCIA_ACCESO = Duration.ofMinutes(5);

    private final MinioClient minio;
    private final MinioClient minioPublico;
    private final String bucket;

    public AlmacenamientoService(MinioClient minio, @Qualifier("minioClientPublico") MinioClient minioPublico,
            @Value("${app.minio.bucket-name}") String bucket) {
        this.minio = minio;
        this.minioPublico = minioPublico;
        this.bucket = bucket;
    }

    /** Guarda un objeto en `ruta` (reemplaza el que hubiera). */
    public void subir(String ruta, byte[] contenido, String mimeType) {
        try (InputStream entrada = new ByteArrayInputStream(contenido)) {
            minio.putObject(PutObjectArgs.builder().bucket(bucket).object(ruta)
                    .stream(entrada, contenido.length, -1).contentType(mimeType).build());
        } catch (Exception e) {
            throw fallo("subir", e);
        }
    }

    /** Si hay un objeto en `ruta`. Un fallo distinto de «no existe» sube como excepcion. */
    public boolean existe(String ruta) {
        try {
            minio.statObject(StatObjectArgs.builder().bucket(bucket).object(ruta).build());
            return true;
        } catch (ErrorResponseException e) {
            String codigo = e.errorResponse() == null ? null : e.errorResponse().code();
            if ("NoSuchKey".equals(codigo) || "NoSuchObject".equals(codigo)) {
                return false;
            }
            throw fallo("consultar", e);
        } catch (Exception e) {
            throw fallo("consultar", e);
        }
    }

    /** Direccion para abrir el objeto durante `vigencia` (menos de 7 dias). No consulta la red. */
    public String urlTemporal(String ruta, Duration vigencia) {
        try {
            return minioPublico.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET).bucket(bucket).object(ruta)
                    .expiry((int) vigencia.toSeconds()).build());
        } catch (Exception e) {
            throw fallo("firmar", e);
        }
    }

    private static AlmacenamientoException fallo(String operacion, Exception causa) {
        LOG.error("Fallo el almacenamiento de archivos", kv("module", "almacenamiento"),
                kv("operacion", operacion), kv("error", causa.getClass().getSimpleName()));
        return new AlmacenamientoException("No se pudo completar la operación de almacenamiento (" + operacion + ")",
                causa);
    }
}
