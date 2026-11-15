package com.comedoruagrm.backend.features.convocatorias.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comedoruagrm.backend.features.convocatorias.model.ConvocatoriaDocumento;
import com.comedoruagrm.backend.features.convocatorias.model.ConvocatoriaDocumentoId;

public interface ConvocatoriaDocumentoRepository
        extends JpaRepository<ConvocatoriaDocumento, ConvocatoriaDocumentoId> {

    /** Los documentos vigentes de una convocatoria, con su tipo, ordenados por nombre. */
    @Query("""
            select d from ConvocatoriaDocumento d
            join fetch d.tipoDocumento t
            where d.convocatoria.id = :convocatoriaId
            order by t.nombre
            """)
    List<ConvocatoriaDocumento> findByConvocatoriaId(@Param("convocatoriaId") Long convocatoriaId);

    /**
     * Vuelve a activar un par que estaba eliminado logicamente. Devuelve las filas afectadas: 1 si el par
     * existia eliminado, 0 si no (entonces hay que insertarlo).
     *
     * ! La clave primaria incluye a las filas eliminadas, y el `@SQLRestriction` las oculta: por eso es una
     * ! consulta nativa y no un `save`.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            update convocatoria_documento
               set eliminado_en = null, obligatorio = :obligatorio, actualizado_en = now()
             where convocatoria_id = :convocatoriaId
               and tipo_documento_id = :tipoDocumentoId
               and eliminado_en is not null
            """, nativeQuery = true)
    int reactivar(@Param("convocatoriaId") Long convocatoriaId, @Param("tipoDocumentoId") Long tipoDocumentoId,
            @Param("obligatorio") boolean obligatorio);
}
