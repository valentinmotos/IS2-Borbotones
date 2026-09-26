package com.zero.ecommerce.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.ActualizacionPrecioDTO;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.CategoriaRepository;
import com.zero.ecommerce.repositories.ProductoRepository;
import com.zero.ecommerce.repositories.SubCategoriaRepository;
import com.zero.ecommerce.repositories.VigenciaPrecioRepository;

@Service
@Transactional(readOnly = true)
public class VigenciaPrecioService {

    private final VigenciaPrecioRepository repository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubCategoriaRepository subCategoriaRepository;

    public VigenciaPrecioService(VigenciaPrecioRepository repository, ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository, SubCategoriaRepository subCategoriaRepository) {
        this.repository = repository;
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.subCategoriaRepository = subCategoriaRepository;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public VigenciaPrecio crearVigenciaPrecio(String idProducto, LocalDate fechaDesde, double precio)
            throws ErrorServiceException {
        validarFechaYPrecio(fechaDesde, precio);
        validarProducto(idProducto);

        Producto producto = productoRepository.findByIdAndEliminadoFalse(idProducto)
                .orElseThrow(() -> new ErrorServiceException("El producto no existe o fue eliminado."));

        List<VigenciaPrecio> vigencias = repository.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc(idProducto);
        for (VigenciaPrecio vigencia : vigencias) {
            if (vigencia.getFechaHasta() == null) {
                vigencia.setFechaHasta(fechaDesde.minusDays(1));
                repository.save(vigencia);
                break;
            }
        }

        VigenciaPrecio nueva = new VigenciaPrecio();
        nueva.setProducto(producto);
        nueva.setFechaDesde(fechaDesde);
        nueva.setFechaHasta(null);
        nueva.setPrecio(precio);
        return repository.save(nueva);
    }

    public VigenciaPrecio buscarVigenciaVigente(String idProducto) throws ErrorServiceException {
        validarProducto(idProducto);
        return repository.findByProducto_IdAndEliminadoFalseAndFechaHastaIsNull(idProducto)
                .orElseThrow(() -> new ErrorServiceException("El producto no tiene un precio vigente."));
    }

    public List<VigenciaPrecio> listarVigencias(String idProducto) throws ErrorServiceException {
        validarProducto(idProducto);
        return repository.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeAsc(idProducto).stream()
                .sorted(Comparator.comparing(VigenciaPrecio::getFechaDesde).reversed())
                .toList();
    }

    public double buscarPrecioVigente(String idProducto) throws ErrorServiceException {
        return buscarVigenciaVigente(idProducto).getPrecio();
    }

    public List<ActualizacionPrecioDTO> previsualizarActualizacionMasiva(String alcance, String idAlcance,
            double porcentaje, LocalDate fechaDesde) throws ErrorServiceException {
        validarActualizacionMasiva(porcentaje, fechaDesde);
        List<Producto> productos = buscarProductosPorAlcance(alcance, idAlcance);
        if (productos.isEmpty()) {
            throw new ErrorServiceException("No hay productos activos en el alcance seleccionado.");
        }

        Set<String> productoIds = productos.stream().map(Producto::getId).collect(java.util.stream.Collectors.toSet());
        Map<String, VigenciaPrecio> vigenciasActuales = new HashMap<>();
        for (VigenciaPrecio vigencia : repository.findByEliminadoFalseAndFechaHastaIsNull()) {
            String productoId = vigencia.getProducto() == null ? null : vigencia.getProducto().getId();
            if (productoIds.contains(productoId) && vigenciasActuales.putIfAbsent(productoId, vigencia) != null) {
                throw new ErrorServiceException("Hay más de un precio vigente para un producto del catálogo.");
            }
        }

        List<ActualizacionPrecioDTO> actualizaciones = new ArrayList<>();
        for (Producto producto : productos) {
            VigenciaPrecio vigente = vigenciasActuales.get(producto.getId());
            if (vigente == null) {
                throw new ErrorServiceException("El producto " + producto.getNombre()
                        + " no tiene un precio vigente. Asignale uno desde Precios antes de actualizar.");
            }
            if (vigente.getFechaDesde() == null || !fechaDesde.isAfter(vigente.getFechaDesde())) {
                throw new ErrorServiceException("La fecha desde debe ser posterior al inicio del precio vigente de "
                        + producto.getNombre() + ".");
            }
            double nuevoPrecio = calcularPrecioAumentado(vigente.getPrecio(), porcentaje);
            actualizaciones.add(new ActualizacionPrecioDTO(producto.getId(), producto.getCodigo(), producto.getNombre(),
                    producto.getTalle(), vigente.getPrecio(), nuevoPrecio, nuevoPrecio - vigente.getPrecio()));
        }
        return actualizaciones;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public int actualizarPreciosMasivo(String alcance, String idAlcance, double porcentaje, LocalDate fechaDesde)
            throws ErrorServiceException {
        List<ActualizacionPrecioDTO> actualizaciones = previsualizarActualizacionMasiva(alcance, idAlcance,
                porcentaje, fechaDesde);
        return aplicarActualizaciones(actualizaciones, fechaDesde);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public int confirmarActualizacionMasiva(String alcance, String idAlcance, double porcentaje,
            LocalDate fechaDesde, List<ActualizacionPrecioDTO> vistaPrevia) throws ErrorServiceException {
        if (vistaPrevia == null || vistaPrevia.isEmpty()) {
            throw new ErrorServiceException("Primero generá una vista previa de la actualización.");
        }

        List<ActualizacionPrecioDTO> actualizaciones = previsualizarActualizacionMasiva(alcance, idAlcance,
                porcentaje, fechaDesde);
        if (!actualizaciones.equals(vistaPrevia)) {
            throw new ErrorServiceException(
                    "Los precios cambiaron desde la vista previa. Revisá los valores antes de confirmar.");
        }

        return aplicarActualizaciones(actualizaciones, fechaDesde);
    }

    private int aplicarActualizaciones(List<ActualizacionPrecioDTO> actualizaciones, LocalDate fechaDesde)
            throws ErrorServiceException {
        for (ActualizacionPrecioDTO actualizacion : actualizaciones) {
            crearVigenciaPrecio(actualizacion.productoId(), fechaDesde, actualizacion.precioNuevo());
        }
        return actualizaciones.size();
    }

    private void validarActualizacionMasiva(double porcentaje, LocalDate fechaDesde) throws ErrorServiceException {
        if (!Double.isFinite(porcentaje) || porcentaje <= 0) {
            throw new ErrorServiceException("El porcentaje de aumento debe ser mayor a 0.");
        }
        if (fechaDesde == null || fechaDesde.isBefore(LocalDate.now())) {
            throw new ErrorServiceException("La fecha desde de la actualización no puede ser anterior a hoy.");
        }
    }

    private List<Producto> buscarProductosPorAlcance(String alcance, String idAlcance)
            throws ErrorServiceException {
        List<Producto> productos = productoRepository.findByEliminadoFalseOrderByNombreAscTalleAsc();
        if ("TODO".equals(alcance)) {
            return productos;
        }
        if ("CATEGORIA".equals(alcance)) {
            if (idAlcance == null || idAlcance.isBlank()) {
                throw new ErrorServiceException("Seleccioná una categoría para actualizar los precios.");
            }
            Categoria categoria = categoriaRepository.findByIdAndEliminadoFalse(idAlcance)
                    .orElseThrow(() -> new ErrorServiceException("La categoría no existe o fue eliminada."));
            return productos.stream()
                    .filter(producto -> producto.getSubCategoria() != null
                            && producto.getSubCategoria().getCategoria() != null
                            && categoria.getId().equals(producto.getSubCategoria().getCategoria().getId()))
                    .toList();
        }
        if ("SUBCATEGORIA".equals(alcance)) {
            if (idAlcance == null || idAlcance.isBlank()) {
                throw new ErrorServiceException("Seleccioná una subcategoría para actualizar los precios.");
            }
            SubCategoria subCategoria = subCategoriaRepository.findByIdAndEliminadoFalse(idAlcance)
                    .orElseThrow(() -> new ErrorServiceException("La subcategoría no existe o fue eliminada."));
            if (subCategoria.getCategoria() == null || subCategoria.getCategoria().isEliminado()) {
                throw new ErrorServiceException("La categoría de la subcategoría seleccionada fue eliminada.");
            }
            return productos.stream()
                    .filter(producto -> producto.getSubCategoria() != null
                            && subCategoria.getId().equals(producto.getSubCategoria().getId()))
                    .toList();
        }
        throw new ErrorServiceException("El alcance de la actualización no es válido.");
    }

    private double calcularPrecioAumentado(double precioActual, double porcentaje) throws ErrorServiceException {
        if (!Double.isFinite(precioActual) || precioActual <= 0) {
            throw new ErrorServiceException("El precio vigente debe ser mayor a 0 para aplicar el aumento.");
        }
        double nuevoPrecio = BigDecimal.valueOf(precioActual)
                .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(porcentaje).movePointLeft(2)))
                .setScale(-1, RoundingMode.HALF_UP)
                .doubleValue();
        if (!Double.isFinite(nuevoPrecio) || nuevoPrecio <= 0) {
            throw new ErrorServiceException("El precio calculado está fuera del rango permitido.");
        }
        return nuevoPrecio;
    }

    private void validarFechaYPrecio(LocalDate fechaDesde, double precio) throws ErrorServiceException {
        if (precio <= 0) {
            throw new ErrorServiceException("El precio de la vigencia debe ser mayor a 0.");
        }
        if (fechaDesde == null || fechaDesde.isBefore(LocalDate.now())) {
            throw new ErrorServiceException("La fecha desde de la vigencia no puede ser anterior a hoy.");
        }
    }

    private void validarProducto(String idProducto) throws ErrorServiceException {
        if (idProducto == null || idProducto.isBlank()) {
            throw new ErrorServiceException("El producto no existe o fue eliminado.");
        }
        if (!productoRepository.existsByIdAndEliminadoFalse(idProducto)) {
            throw new ErrorServiceException("El producto no existe o fue eliminado.");
        }
    }
}
