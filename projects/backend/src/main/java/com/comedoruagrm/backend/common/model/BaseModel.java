package com.comedoruagrm.backend.common.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Base para todos los Models del proyecto: codigo (UUID), createdAt,
 * updatedAt y soft delete via deletedAt.
 *
 * * El @SQLRestriction filtra "deleted_at IS NULL" en TODA query que
 * * Hibernate genere para la entidad (findAll, findById, JPQL, fetch de
 * * relaciones) -- es estatico, no se puede "sacar" en una query puntual.
 * * Si algun modulo necesita ver soft-deleted (ej. una papelera), se le
 * * agrega un @Filter opcional a esa entidad puntual, no se cambia este
 * * mecanismo global.
 */
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
public abstract class BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "codigo", updatable = false, nullable = false)
    private UUID codigo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getCodigo() {
        return codigo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
