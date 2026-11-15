package com.comedoruagrm.backend.features.postulaciones.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comedoruagrm.backend.features.postulaciones.model.DocumentoAdjunto;

/** Acceso a la tabla `documento_adjunto` (CU16). Las lecturas de la lista van por {@link DocumentosPostulacionConsultaRepository}. */
public interface DocumentoAdjuntoRepository extends JpaRepository<DocumentoAdjunto, Long> {

    /** El documento de esa postulacion con ese codigo publico; vacio si no existe, esta eliminado o es de otra. */
    Optional<DocumentoAdjunto> findByCodigoAndPostulacionId(UUID codigo, Long postulacionId);

    /** Cuantos archivos vigentes tiene la postulacion de ese tipo de documento. */
    long countByPostulacionIdAndTipoDocumentoId(Long postulacionId, Long tipoDocumentoId);
}
