package com.comedoruagrm.backend.features.postulaciones.model;

import java.time.Instant;

import com.comedoruagrm.backend.common.model.BaseModel;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un archivo adjunto a una postulacion (CU16): un documento de respaldo de un tipo que exige la convocatoria.
 *
 * * El binario vive en el almacenamiento (MinIO o S3); aca solo su ruta, tipo y tamaño.
 * * `postulacionId` y `tipoDocumentoId` son ids simples (el modulo no depende de las entidades ajenas).
 * * Reemplazar un archivo cambia sus datos en la misma fila (`reemplazarArchivo`); eliminarlo es el borrado
 * * logico de `eliminadoEn`.
 * ! `storagePath` nunca se expone en un DTO.
 */
@Getter
@Entity
@Table(name = "documento_adjunto")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_documento_adjunto"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentoAdjunto extends BaseModel {

    @Column(name = "postulacion_id", nullable = false, updatable = false)
    private Long postulacionId;

    @Column(name = "tipo_documento_id", nullable = false, updatable = false)
    private Long tipoDocumentoId;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "subido_en", nullable = false)
    private Instant subidoEn;

    public DocumentoAdjunto(Long postulacionId, Long tipoDocumentoId, String nombreArchivo, String mimeType,
            String storagePath, long tamanoBytes, Instant subidoEn) {
        this.postulacionId = postulacionId;
        this.tipoDocumentoId = tipoDocumentoId;
        this.nombreArchivo = nombreArchivo;
        this.mimeType = mimeType;
        this.storagePath = storagePath;
        this.tamanoBytes = tamanoBytes;
        this.subidoEn = subidoEn;
    }

    /** Cambia el archivo por otro (CU16, reemplazar). El tipo de documento no cambia. */
    public void reemplazarArchivo(String nombreArchivo, String mimeType, String storagePath, long tamanoBytes,
            Instant subidoEn) {
        this.nombreArchivo = nombreArchivo;
        this.mimeType = mimeType;
        this.storagePath = storagePath;
        this.tamanoBytes = tamanoBytes;
        this.subidoEn = subidoEn;
    }
}
