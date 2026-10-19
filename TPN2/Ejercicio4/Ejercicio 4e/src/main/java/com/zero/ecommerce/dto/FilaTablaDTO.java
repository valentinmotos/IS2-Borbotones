package com.zero.ecommerce.dto;

import java.util.List;

/**
 * Fila de fragments/tabla :: tablaRegistros. El nombre identifica al registro en las
 * etiquetas de accesibilidad y en el modal de baja; las celdas se muestran en orden.
 */
public record FilaTablaDTO(String id, String nombre, List<String> celdas) {
}
