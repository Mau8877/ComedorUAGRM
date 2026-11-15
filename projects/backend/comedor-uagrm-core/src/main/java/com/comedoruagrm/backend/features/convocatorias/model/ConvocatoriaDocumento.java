package com.comedoruagrm.backend.features.convocatorias.model;

import com.comedoruagrm.backend.common.model.BaseModelSinId;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un documento que exige una convocatoria y si es obligatorio. La clave es el par (convocatoria, tipo de
 * documento); no tiene `id` propio, asi que extiende BaseModelSinId (ver PERSISTENCIA_BD_BACKEND.md).
 */
@Getter
@Entity
@Table(name = "convocatoria_documento")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_convocatoria_documento"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConvocatoriaDocumento extends BaseModelSinId {

    @EmbeddedId
    private ConvocatoriaDocumentoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("convocatoriaId")
    @JoinColumn(name = "convocatoria_id", nullable = false)
    private Convocatoria convocatoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tipoDocumentoId")
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @Setter
    @Column(name = "obligatorio", nullable = false)
    private boolean obligatorio;

    public ConvocatoriaDocumento(Convocatoria convocatoria, TipoDocumento tipoDocumento, boolean obligatorio) {
        this.id = new ConvocatoriaDocumentoId(convocatoria.getId(), tipoDocumento.getId());
        this.convocatoria = convocatoria;
        this.tipoDocumento = tipoDocumento;
        this.obligatorio = obligatorio;
    }
}
