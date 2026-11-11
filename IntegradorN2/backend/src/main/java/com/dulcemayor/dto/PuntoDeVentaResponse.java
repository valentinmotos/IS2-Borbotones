package com.dulcemayor.dto;

public record PuntoDeVentaResponse(String id, String nombreComercio, String minoristaId,
        String nombreMinorista, String idLocalidad, String localidad, String idDepartamento,
        String departamento, String codigoPostal, String calle, String numeracion, String barrio,
        String manzanaPiso, String casaDepartamento, String referencia, double latitud, double longitud) {}
