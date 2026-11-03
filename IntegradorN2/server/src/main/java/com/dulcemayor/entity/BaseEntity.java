package com.dulcemayor.entity;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Superclase de todas las entidades (patrón OID, igual que IntegradorN1): id UUID generado
 * por Hibernate, que no se modifica nunca, y marca de baja lógica. Ninguna entidad se borra
 * físicamente: "eliminar" es poner eliminado = true.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private boolean eliminado = false;
}
