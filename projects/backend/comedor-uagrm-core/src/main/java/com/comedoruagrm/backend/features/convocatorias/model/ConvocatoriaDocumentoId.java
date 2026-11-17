package com.comedoruagrm.backend.features.convocatorias.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Clave compuesta de `convocatoria_documento`: la convocatoria y el tipo de documento.
 *
 * ! La clave incluye a las filas eliminadas logicamente: volver a agregar un par ya eliminado se hace
 * ! reactivando la fila (ConvocatoriaDocumentoRepository.reactivar), no insertando otra.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConvocatoriaDocumentoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "convocatoria_id", nullable = false)
    private Long convocatoriaId;

    @Column(name = "tipo_documento_id", nullable = false)
    private Long tipoDocumentoId;

    public ConvocatoriaDocumentoId(Long convocatoriaId, Long tipoDocumentoId) {
        this.convocatoriaId = convocatoriaId;
        this.tipoDocumentoId = tipoDocumentoId;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ConvocatoriaDocumentoId that)) {
            return false;
        }
        return Objects.equals(convocatoriaId, that.convocatoriaId)
                && Objects.equals(tipoDocumentoId, that.tipoDocumentoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(convocatoriaId, tipoDocumentoId);
    }
}
