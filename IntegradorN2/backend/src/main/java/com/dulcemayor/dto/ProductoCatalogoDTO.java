package com.dulcemayor.dto;

/**
 * E3-01: producto del catálogo público (contrato del kickoff: id, codigo, nombre, presentacion,
 * precio, stock, imagenId; más descripción, unidades por bulto y categoría para la pantalla).
 * Solo se arma para productos con stock y precio vigente. Lo consumen E3-03 y E4-02.
 *
 * @param precio precio mayorista vigente por bulto, en pesos
 * @param stock  bultos disponibles (siempre mayor que cero)
 */
public record ProductoCatalogoDTO(String id, String codigo, String nombre, String descripcion,
        String presentacion, int unidadesPorBulto, double precio, int stock, String categoriaId,
        String categoriaNombre, String imagenId) {
}
