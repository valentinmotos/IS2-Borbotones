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
     * Contrato base de E3-05: devuelve unicamente productos activos, de categorias
     * activas, con stock positivo y precio vigente.
     */
    public List<ProductoCatalogoDTO> listar(CatalogoFiltro filtro) {
        CatalogoFiltro alcance = filtro == null ? CatalogoFiltro.todos() : filtro;
        List<ProductoCatalogoDTO> resultado = new ArrayList<>();
        for (Producto producto : productoService.listarProductoActivo()) {
            if (coincide(producto, alcance)) {
                convertirSiVisible(producto).ifPresent(resultado::add);
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
