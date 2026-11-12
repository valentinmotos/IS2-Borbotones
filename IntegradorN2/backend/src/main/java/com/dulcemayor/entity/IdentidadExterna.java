package com.dulcemayor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Vínculo de autenticación revocable entre una cuenta local y un proveedor OpenID Connect. */
@Entity
@Table(name = "identidades_externas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_identidad_emisor_sujeto", columnNames = { "emisor", "sujeto" }),
        @UniqueConstraint(name = "uk_identidad_usuario_emisor", columnNames = { "usuario_id", "emisor" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdentidadExterna {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String emisor;

    @Column(nullable = false)
    private String sujeto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public IdentidadExterna(String emisor, String sujeto, Usuario usuario) {
        this.emisor = emisor;
        this.sujeto = sujeto;
        this.usuario = usuario;
    }
}
