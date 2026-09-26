package com.zero.ecommerce.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.CatalogoFiltro;
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.utils.TextoUtils;

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
     * Devuelve únicamente productos publicables: activos, con stock y con precio
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
                resultado.add(toDTO(producto, precio, stock));
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

    public List<ProductoCatalogoDTO> buscar(String texto, CatalogoFiltro filtro) {
        CatalogoFiltro alcance = filtro == null ? CatalogoFiltro.todos() : filtro;

        String textoBuscado = (texto == null || texto.isBlank())
                ? null
                : TextoUtils.normalizar(texto);

        String talleNorm = (alcance.talle() == null || alcance.talle().isBlank())
                ? null
                : alcance.talle().strip().toLowerCase();

        List<ProductoCatalogoDTO> resultado = new ArrayList<>();

        for (Producto producto : productoService.listarProductoActivo()) {
            if (!coincide(producto, alcance)) {
                continue;
            }

            if (textoBuscado != null) {
                boolean enNombre = contiene(producto.getNombre(), textoBuscado);
                boolean enDescripcion = contiene(producto.getDescripcion(), textoBuscado);
                if (!enNombre && !enDescripcion) {
                    continue;
                }
            }

            if (talleNorm != null
                    && (producto.getTalle() == null
                            || !producto.getTalle().strip().toLowerCase().equals(talleNorm))) {
                continue;
            }

            if (Boolean.TRUE.equals(alcance.soloOfertas()) && !producto.isEnOferta()) {
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

                if (alcance.precioMin() != null && precio < alcance.precioMin()) {
                    continue;
                }
                if (alcance.precioMax() != null && precio > alcance.precioMax()) {
                    continue;
                }

                resultado.add(toDTO(producto, precio, stock));
            } catch (ErrorServiceException ignored) {
            }
        }

        return ordenar(resultado, alcance.orden());
    }

    public List<ProductoCatalogoDTO> buscarEnCategoria(String categoriaId, String subCategoriaId,
            String texto, Double precioMin, Double precioMax, String talle,
            Boolean soloOfertas, String orden) {
        CatalogoFiltro filtro = CatalogoFiltro.busquedaEnCategoria(
                categoriaId, subCategoriaId, precioMin, precioMax, talle, soloOfertas, orden);
        return buscar(texto, filtro);
    }

    private List<ProductoCatalogoDTO> ordenar(List<ProductoCatalogoDTO> lista, String orden) {
        if (lista.isEmpty() || orden == null || orden.isBlank()) {
            return List.copyOf(lista);
        }
        Comparator<ProductoCatalogoDTO> comparador = switch (orden.toLowerCase()) {
            case "precio_asc"  -> Comparator.comparingDouble(ProductoCatalogoDTO::precio);
            case "precio_desc" -> Comparator.comparingDouble(ProductoCatalogoDTO::precio).reversed();
            case "nombre"      -> Comparator.comparing(p -> TextoUtils.normalizar(p.nombre()));
            default            -> null;
        };
        if (comparador == null) {
            return List.copyOf(lista);
        }
        return lista.stream().sorted(comparador).toList();
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

    private boolean contiene(String valor, String buscado) {
        return valor != null && TextoUtils.normalizar(valor).contains(buscado);
    }

    private ProductoCatalogoDTO toDTO(Producto producto, double precio, int stock) {
        return new ProductoCatalogoDTO(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getTalle(),
                producto.getImagen() == null ? null : producto.getImagen().getId(),
                precio,
                producto.isEnOferta(),
                stock);
    }
}
