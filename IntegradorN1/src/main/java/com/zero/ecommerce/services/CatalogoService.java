package com.zero.ecommerce.services;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.CatalogoFiltro;
import com.zero.ecommerce.dto.FiltroCatalogoDTO;
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
    public Page<ProductoCatalogoDTO> listarOfertas(FiltroCatalogoDTO filtro, int pagina, int tamanio) {
        FiltroCatalogoDTO seguro = filtro == null ? new FiltroCatalogoDTO(null, null, null, null) : filtro;
        List<ProductoCatalogoDTO> ofertas = listar(CatalogoFiltro.ofertas()).stream()
                .filter(p -> seguro.precioMinimo() == null || p.precio() >= seguro.precioMinimo())
                .filter(p -> seguro.precioMaximo() == null || p.precio() <= seguro.precioMaximo())
                .filter(p -> vacio(seguro.talle()) || p.talle().equalsIgnoreCase(seguro.talle().strip()))
                .sorted(comparador(seguro.ordenSeguro()))
                .toList();
        return paginar(ofertas, pagina, tamanio);
    }

    public ProductoCatalogoDTO buscarDetalle(String id) throws ErrorServiceException {
        Producto producto;
        try {
            producto = productoService.buscarProducto(id);
        } catch (ErrorServiceException e) {
            throw noDisponible();
        }
        return convertirSiVisible(producto).orElseThrow(this::noDisponible);
    }

    /** Variantes vendibles del mismo modelo, incluida la actual para marcarla en la vista. */
    public List<ProductoCatalogoDTO> listarTallesDelModelo(ProductoCatalogoDTO producto) {
        return listar(CatalogoFiltro.todos()).stream()
                .filter(p -> p.nombre().equalsIgnoreCase(producto.nombre()))
                .sorted(Comparator.comparing(ProductoCatalogoDTO::talle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<ProductoCatalogoDTO> listarRelacionados(ProductoCatalogoDTO producto, int limite) {
        return listar(CatalogoFiltro.todos()).stream()
                .filter(p -> !p.id().equals(producto.id()))
                .filter(p -> p.subCategoriaId().equals(producto.subCategoriaId()))
                .filter(p -> !p.nombre().equalsIgnoreCase(producto.nombre()))
                .limit(Math.max(0, limite))
                .toList();
    }

    public List<String> listarTallesDisponiblesEnOferta() {
        return listar(CatalogoFiltro.ofertas()).stream()
                .map(ProductoCatalogoDTO::talle)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Optional<ProductoCatalogoDTO> convertirSiVisible(Producto producto) {
        if (producto.getSubCategoria() == null || producto.getSubCategoria().isEliminado()
                || producto.getSubCategoria().getCategoria() == null
                || producto.getSubCategoria().getCategoria().isEliminado()) {
            return Optional.empty();
        }
        int stock = stockService.buscarStockActual(producto.getId());
        if (stock <= 0) {
            return Optional.empty();
        }
        try {
            double precio = vigenciaPrecioService.buscarPrecioVigente(producto.getId());
            if (!Double.isFinite(precio) || precio <= 0) {
                return Optional.empty();
            }
            return Optional.of(new ProductoCatalogoDTO(
                    producto.getId(), producto.getCodigo(), producto.getNombre(), producto.getDescripcion(),
                    producto.getTalle(), producto.getImagen() == null ? null : producto.getImagen().getId(),
                    precio, formatearPrecio(precio), producto.isEnOferta(), stock,
                    producto.getSubCategoria().getCategoria().getNombre(), producto.getSubCategoria().getNombre(),
                    producto.getSubCategoria().getId()));
        } catch (ErrorServiceException e) {
            return Optional.empty();
        }
    }

    private boolean coincide(Producto producto, CatalogoFiltro filtro) {
        if (producto.getSubCategoria() == null || producto.getSubCategoria().getCategoria() == null
                || producto.getSubCategoria().isEliminado()
                || producto.getSubCategoria().getCategoria().isEliminado()) {
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
    private Comparator<ProductoCatalogoDTO> comparador(String orden) {
        Comparator<ProductoCatalogoDTO> porNombre = Comparator
                .comparing(ProductoCatalogoDTO::nombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ProductoCatalogoDTO::talle, String.CASE_INSENSITIVE_ORDER);
        return switch (orden) {
            case FiltroCatalogoDTO.ORDEN_PRECIO_ASC -> Comparator.comparingDouble(ProductoCatalogoDTO::precio)
                    .thenComparing(porNombre);
            case FiltroCatalogoDTO.ORDEN_PRECIO_DESC -> Comparator.comparingDouble(ProductoCatalogoDTO::precio)
                    .reversed().thenComparing(porNombre);
            default -> porNombre;
        };
    }

    private Page<ProductoCatalogoDTO> paginar(List<ProductoCatalogoDTO> productos, int pagina, int tamanio) {
        int tamanioSeguro = Math.max(1, tamanio);
        int totalPaginas = Math.max(1, (int) Math.ceil((double) productos.size() / tamanioSeguro));
        int paginaSegura = Math.min(Math.max(1, pagina), totalPaginas);
        int desde = Math.min((paginaSegura - 1) * tamanioSeguro, productos.size());
        int hasta = Math.min(desde + tamanioSeguro, productos.size());
        return new PageImpl<>(productos.subList(desde, hasta), PageRequest.of(paginaSegura - 1, tamanioSeguro),
                productos.size());
    }

    private String formatearPrecio(double precio) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "AR"));
        formato.setMinimumFractionDigits(0);
        formato.setMaximumFractionDigits(2);
        return "$" + formato.format(precio);
    }

    private ErrorServiceException noDisponible() {
        return new ErrorServiceException("El producto no existe o no esta disponible.");
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
