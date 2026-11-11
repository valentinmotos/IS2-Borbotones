package com.dulcemayor.dto;

/**
 * E3-04: producto pedido en el chat que quedó fuera del presupuesto.
 *
 * @param producto nombre del catálogo, o el que escribió el visitante si no se encontró
 * @param cantidad cantidad pedida
 * @param motivo   por qué se excluyó: no está en el catálogo, sin stock, stock insuficiente o sin precio
 */
public record ProductoExcluidoDTO(String producto, int cantidad, String motivo) {
}
