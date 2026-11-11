package com.dulcemayor.dto;

/** E2-06: punto de venta con su dirección, su zona (departamento) y su ubicación en el mapa. */
public record PuntoDeVentaResponse(String id, String nombreComercio, String minoristaId,
        String nombreMinorista, String preventorId, String idLocalidad, String localidad, String idDepartamento,
        String departamento, String codigoPostal, String calle, String numeracion, String barrio,
        String manzanaPiso, String casaDepartamento, String referencia, double latitud, double longitud) {
}
