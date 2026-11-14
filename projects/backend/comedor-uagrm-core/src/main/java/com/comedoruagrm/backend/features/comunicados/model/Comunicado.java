package com.comedoruagrm.backend.features.comunicados.model;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.comedoruagrm.backend.common.model.BaseModel;
import com.comedoruagrm.backend.features.comunicados.enums.DestinatariosComunicado;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Comunicado publicado por la DUBSS (CU21). Una vez publicado no se edita ni se borra desde el CU:
 * es lo que ya recibieron los destinatarios, por eso no tiene setters.
 *
 * * `convocatoriaId` y `publicadoPor` son ids simples (este modulo no depende de las entidades ajenas).
 */
@Getter
@Entity
@Table(name = "comunicado")
@AttributeOverride(name = "codigo", column = @Column(name = "codigo_comunicado"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comunicado extends BaseModel {

    @Column(name = "convocatoria_id", updatable = false)
    private Long convocatoriaId;

    @Column(name = "publicado_por", nullable = false, updatable = false)
    private Long publicadoPor;

    @Column(name = "titulo", nullable = false, length = 150, updatable = false)
    private String titulo;

    @Column(name = "contenido", nullable = false, columnDefinition = "text", updatable = false)
    private String contenido;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "destinatarios", nullable = false, columnDefinition = "destinatarios_comunicado",
            updatable = false)
    private DestinatariosComunicado destinatarios;

    @Column(name = "publicado_en", nullable = false, updatable = false)
    private Instant publicadoEn;

    public Comunicado(Long convocatoriaId, Long publicadoPor, String titulo, String contenido,
            DestinatariosComunicado destinatarios, Instant publicadoEn) {
        this.convocatoriaId = convocatoriaId;
        this.publicadoPor = publicadoPor;
        this.titulo = titulo;
        this.contenido = contenido;
        this.destinatarios = destinatarios;
        this.publicadoEn = publicadoEn;
    }
}
