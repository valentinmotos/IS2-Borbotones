package com.zero.ecommerce.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.CatalogoFiltro;
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.exception.ErrorServiceException;

@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final ProductoService productoService;
    private final StockService stockService;
    private final VigenciaPrecioService vigenciaPrecioService;

    public CatalogoService(ProductoService productoService, StockService stockService,
            VigenciaPrecioService vigenciaPrecioService) {
        this.productoService = productoService;
        this.stockService = stockService;
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    /**
     * Devuelve unicamente productos publicables: activos, con stock y con precio
     * vigente. La ausencia de precio oculta el producto en lugar de romper toda la
     * vidriera.
     */
    public List<ProductoCatalogoDTO> listar(CatalogoFiltro filtro) {
        CatalogoFiltro alcance = filtro == null ? CatalogoFiltro.todos() : filtro;
        List<ProductoCatalogoDTO> resultado = new ArrayList<>();

        for (Producto producto : productoService.listarProductoActivo()) {
            if (!coincide(producto, alcance)) {
                continue;
            }

            int stock = stockService.buscarStockActual(producto.getId());
            if (stock <= 0) {
                continue;
            }

            try {
                double precio = vigenciaPrecioService.buscarPrecioVigente(producto.getId());
                if (!Double.isFinite(precio) || precio <= 0) {
                    continue;
                }
                resultado.add(new ProductoCatalogoDTO(
                        producto.getId(),
                        producto.getCodigo(),
                        producto.getNombre(),
                        producto.getTalle(),
                        producto.getImagen() == null ? null : producto.getImagen().getId(),
                        precio,
                        producto.isEnOferta(),
                        stock));
            } catch (ErrorServiceException e) {
                // Un producto sin vigencia abierta no forma parte del catalogo publico.
            }
        }
        return List.copyOf(resultado);
    }

    public List<ProductoCatalogoDTO> listarPorCategoria(String categoriaId) {
        return listar(CatalogoFiltro.porCategoria(categoriaId));
    }

    public List<ProductoCatalogoDTO> listarPorSubCategoria(String categoriaId, String subCategoriaId) {
        return listar(CatalogoFiltro.porSubCategoria(categoriaId, subCategoriaId));
    }

    private boolean coincide(Producto producto, CatalogoFiltro filtro) {
        if (producto.getSubCategoria() == null || producto.getSubCategoria().getCategoria() == null) {
            return false;
        }
        if (filtro.categoriaId() != null
                && !filtro.categoriaId().equals(producto.getSubCategoria().getCategoria().getId())) {
            return false;
        }
        if (filtro.subCategoriaId() != null
                && !filtro.subCategoriaId().equals(producto.getSubCategoria().getId())) {
            return false;
        }
        return filtro.soloOfertas() == null || !filtro.soloOfertas() || producto.isEnOferta();
    }
}
