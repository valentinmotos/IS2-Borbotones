package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.RolUsuario;
import com.dulcemayor.entity.enums.TipoDocumento;
import java.time.LocalDate;

public record UsuarioInternoResponse(String id, String correo, RolUsuario rol,
        boolean claveTemporal, boolean activo, String nombre, String apellido,
        LocalDate fechaNacimiento, TipoDocumento tipoDocumento, String numeroDocumento, String legajo) {
}
