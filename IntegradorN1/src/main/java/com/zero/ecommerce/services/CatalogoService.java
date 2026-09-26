package com.zero.ecommerce.services;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.FiltroCatalogoDTO;
import com.zero.ecommerce.dto.ProductoCatalogoDTO;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ProductoRepository;

@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final ProductoRepository productoRepository;
    private final VigenciaPrecioService vigenciaPrecioService;
    private final StockService stockService;

    public CatalogoService(ProductoRepository productoRepository, VigenciaPrecioService vigenciaPrecioService,
            StockService stockService) {
        this.productoRepository = productoRepository;
        this.vigenciaPrecioService = vigenciaPrecioService;
        this.stockService = stockService;
    }

    /** Solo devuelve productos que realmente pueden mostrarse y venderse. */
    public List<ProductoCatalogoDTO> listar(FiltroCatalogoDTO filtro) {
        FiltroCatalogoDTO seguro = filtro == null ? new FiltroCatalogoDTO(null, null, null, null) : filtro;
        return productoRepository.findByEliminadoFalseOrderByNombreAscTalleAsc().stream()
                .map(this::convertirSiVisible)
                .flatMap(java.util.Optional::stream)
                .filter(p -> seguro.precioMinimo() == null || p.precio() >= seguro.precioMinimo())
                .filter(p -> seguro.precioMaximo() == null || p.precio() <= seguro.precioMaximo())
                .filter(p -> vacio(seguro.talle()) || p.talle().equalsIgnoreCase(seguro.talle().strip()))
                .sorted(comparador(seguro.ordenSeguro()))
                .toList();
    }

    public Page<ProductoCatalogoDTO> listarOfertas(FiltroCatalogoDTO filtro, int pagina, int tamanio) {
        List<ProductoCatalogoDTO> ofertas = listar(filtro).stream()
                .filter(ProductoCatalogoDTO::enOferta)
                .toList();
        return paginar(ofertas, pagina, tamanio);
    }

    public ProductoCatalogoDTO buscarDetalle(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw noDisponible();
        }
        Producto producto = productoRepository.findByIdAndEliminadoFalse(id).orElseThrow(this::noDisponible);
        return convertirSiVisible(producto).orElseThrow(this::noDisponible);
    }

    /** Variantes vendibles del mismo modelo, incluida la actual para marcarla en la vista. */
    public List<ProductoCatalogoDTO> listarTallesDelModelo(ProductoCatalogoDTO producto) {
        return listar(null).stream()
                .filter(p -> p.nombre().equalsIgnoreCase(producto.nombre()))
                .sorted(Comparator.comparing(ProductoCatalogoDTO::talle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<ProductoCatalogoDTO> listarRelacionados(ProductoCatalogoDTO producto, int limite) {
        return listar(null).stream()
                .filter(p -> !p.id().equals(producto.id()))
                .filter(p -> p.subCategoriaId().equals(producto.subCategoriaId()))
                .filter(p -> !p.nombre().equalsIgnoreCase(producto.nombre()))
                .limit(Math.max(0, limite))
                .toList();
    }

    public List<String> listarTallesDisponiblesEnOferta() {
        return listar(null).stream()
                .filter(ProductoCatalogoDTO::enOferta)
                .map(ProductoCatalogoDTO::talle)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private java.util.Optional<ProductoCatalogoDTO> convertirSiVisible(Producto producto) {
        if (producto.getSubCategoria() == null || producto.getSubCategoria().isEliminado()
                || producto.getSubCategoria().getCategoria() == null
                || producto.getSubCategoria().getCategoria().isEliminado()) {
            return java.util.Optional.empty();
        }
        int stock = stockService.buscarStockActual(producto.getId());
        if (stock <= 0) {
            return java.util.Optional.empty();
        }
        try {
            VigenciaPrecio vigencia = vigenciaPrecioService.buscarVigenciaVigente(producto.getId());
            if (!vigencia.estaVigente(LocalDate.now()) || vigencia.getPrecio() <= 0) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new ProductoCatalogoDTO(
                    producto.getId(), producto.getCodigo(), producto.getNombre(), producto.getDescripcion(),
                    producto.getTalle(), producto.getImagen() == null ? null : producto.getImagen().getId(),
                    vigencia.getPrecio(), formatearPrecio(vigencia.getPrecio()), producto.isEnOferta(), stock,
                    producto.getSubCategoria().getCategoria().getNombre(), producto.getSubCategoria().getNombre(),
                    producto.getSubCategoria().getId()));
        } catch (ErrorServiceException e) {
            return java.util.Optional.empty();
        }
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
        int desde = (paginaSegura - 1) * tamanioSeguro;
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
