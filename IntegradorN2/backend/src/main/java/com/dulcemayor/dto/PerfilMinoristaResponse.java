package com.dulcemayor.dto;

import java.time.LocalDate;

import com.dulcemayor.entity.enums.TipoDocumento;

/**
 * E2-07: perfil del minorista autenticado. Los campos de la dirección son null mientras no cargue
 * una; {@code departamentoId} y {@code localidadId} sirven para inicializar el componente de E1-04.
 */
public record PerfilMinoristaResponse(String id, String correo, String nombre, String apellido,
        LocalDate fechaNacimiento, TipoDocumento tipoDocumento, String numeroDocumento,
        String departamentoId, String departamento, String localidadId, String localidad, String codigoPostal,
        String calle, String numeracion, String barrio, String manzanaPiso, String casaDepartamento,
        String referencia, Double latitud, Double longitud) {
}
