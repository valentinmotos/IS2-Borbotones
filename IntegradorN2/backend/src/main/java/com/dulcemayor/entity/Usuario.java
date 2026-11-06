package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.ProveedorAuth;
import com.dulcemayor.entity.enums.RolUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuarios_nombre_usuario", columnNames = "nombre_usuario")
})
@Getter
@Setter
public class Usuario extends BaseEntity {

    @Column(name = "nombre_usuario", nullable = false, length = 254)
    private String nombreUsuario;

    @Column(nullable = false, length = 255)
    private String clave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolUsuario rol;

    @Column(name = "clave_temporal", nullable = false)
    private boolean claveTemporal = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor_auth", nullable = false, length = 30)
    private ProveedorAuth proveedorAuth = ProveedorAuth.LOCAL;

    @Column(nullable = false)
    private boolean activo = true;
}
