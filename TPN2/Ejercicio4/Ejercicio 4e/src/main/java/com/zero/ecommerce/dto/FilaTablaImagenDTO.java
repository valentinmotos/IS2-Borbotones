package com.zero.ecommerce.dto;

import java.util.List;

/**
 * Fila de fragments/tabla :: tablaRegistrosImagen: como FilaTablaDTO, con una miniatura en la
 * primera columna. imagenId puede ser null; en ese caso se muestra la imagen por defecto.
 */
public record FilaTablaImagenDTO(String id, String nombre, String imagenId, List<String> celdas) {
}
