package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.TipoEmpresa;

public record SucursalResponse(String id, String nombre, TipoEmpresa tipo, String idMayorista,
        String razonSocial, String idLocalidad, String localidad, String codigoPostal,
        String calle, String numeracion, String barrio, String manzanaPiso,
        String casaDepartamento, String referencia, double latitud, double longitud,
        String idImagen, String urlImagen) {
}
