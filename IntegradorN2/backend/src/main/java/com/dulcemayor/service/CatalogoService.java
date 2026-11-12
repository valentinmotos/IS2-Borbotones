package com.dulcemayor.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.CategoriaResponse;
import com.dulcemayor.dto.ProductoCatalogoDTO;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.ProductoRepository;

/**
 * Catálogo público (E3-01). Consulta stock y precio en cada pedido (sin caché), así un producto
 * que llega a stock 0 deja de mostrarse en la consulta siguiente.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final ProductoRepository productos;
    private final CategoriaService categorias;

    public CatalogoService(ProductoRepository productos, CategoriaService categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    /** Productos disponibles; nombre y categoría vacíos o null = sin filtro. */
    public List<ProductoCatalogoDTO> buscar(String nombre, String categoriaId) {
        return productos.buscarCatalogo("", limpiar(nombre), limpiar(categoriaId), LocalDate.now());
    }

    /** Detalle de un producto disponible (E3-03). 404 si no existe, no tiene stock o no tiene precio. */
    public ProductoCatalogoDTO buscarDisponible(String id) {
        String limpio = limpiar(id);
        if (limpio.isEmpty()) {
            throw new RecursoNoEncontradoException("El producto no está disponible.");
        }
        return productos.buscarCatalogo(limpio, "", "", LocalDate.now()).stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no está disponible."));
    }

    public List<CategoriaResponse> listarCategorias() {
        return categorias.listarCategoriaActivo();
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.strip();
    }
}
