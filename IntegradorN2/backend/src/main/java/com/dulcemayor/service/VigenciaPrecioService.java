package com.dulcemayor.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.dto.VigenciaPrecioRequest;
import com.dulcemayor.dto.VigenciaPrecioResponse;
import com.dulcemayor.dto.mapper.VigenciaPrecioMapper;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.VigenciaPrecio;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.VigenciaPrecioRepository;

/**
 * E2-03: precios mayoristas con vigencias (adaptado de IntegradorN1).
 *
 * Reglas: el precio vigente es la vigencia sin fecha de cierre. Al cargar un precio nuevo, la
 * vigencia abierta se cierra el día anterior a la nueva fecha desde, así las vigencias de un
 * producto nunca se superponen. Las vigencias no se editan ni se borran: un error se corrige
 * cargando un precio nuevo.
 */
@Service
@Transactional(readOnly = true)
public class VigenciaPrecioService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PRODUCTO_NO_ENCONTRADO = "No se encontró el producto.";

    private final VigenciaPrecioRepository vigencias;
    private final ProductoRepository productos;
    private final VigenciaPrecioMapper mapper;

    public VigenciaPrecioService(VigenciaPrecioRepository vigencias, ProductoRepository productos,
            VigenciaPrecioMapper mapper) {
        this.vigencias = vigencias;
        this.productos = productos;
        this.mapper = mapper;
    }

    /** Alta de un precio: cierra la vigencia abierta (si hay) y abre la nueva. */
    @Transactional
    public VigenciaPrecioResponse crearVigenciaPrecio(String idProducto, VigenciaPrecioRequest request) {
        // Bloquea la fila del producto: dos altas simultáneas no pueden dejar dos precios abiertos.
        Producto producto = productoParaActualizar(idProducto);
        Optional<VigenciaPrecio> abierta = buscarAbierta(producto.getId());
        validar(request.fechaDesde(), request.precio(), abierta.orElse(null));

        abierta.ifPresent(anterior -> {
            anterior.setFechaHasta(request.fechaDesde().minusDays(1));
            vigencias.save(anterior);
        });

        VigenciaPrecio nueva = new VigenciaPrecio();
        nueva.setProducto(producto);
        nueva.setFechaDesde(request.fechaDesde());
        nueva.setFechaHasta(null);
        nueva.setPrecio(request.precio());
        return mapper.aRespuesta(vigencias.save(nueva));
    }

    /**
     * La fecha desde no puede ser anterior a hoy y tiene que ser posterior al inicio del precio
     * vigente; así el rango que se cierra siempre queda con desde menor o igual que hasta.
     */
    void validar(LocalDate fechaDesde, Double precio, VigenciaPrecio vigente) {
        if (precio == null || !Double.isFinite(precio) || precio <= 0) {
            throw new ErrorServiceException("El precio debe ser mayor que cero.", "precio");
        }
        if (fechaDesde == null) {
            throw new ErrorServiceException("Ingresá la fecha desde.", "fechaDesde");
        }
        if (fechaDesde.isBefore(LocalDate.now())) {
            throw new ErrorServiceException("La fecha desde no puede ser anterior a hoy.", "fechaDesde");
        }
        if (vigente != null && !fechaDesde.isAfter(vigente.getFechaDesde())) {
            throw new ErrorServiceException("La fecha desde debe ser posterior al inicio del precio vigente ("
                    + vigente.getFechaDesde().format(FORMATO_FECHA) + ").", "fechaDesde");
        }
    }

    /** Precio vigente del producto. 404 si el producto no existe o todavía no tiene precio. */
    public VigenciaPrecioResponse buscarVigenciaPrecioVigente(String idProducto) {
        Producto producto = productoActivo(idProducto);
        return buscarAbierta(producto.getId())
                .map(mapper::aRespuesta)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no tiene un precio vigente."));
    }

    /** Para otros services (catálogo, presupuesto, pedidos): el precio vigente, si el producto tiene uno. */
    public Optional<Double> buscarPrecioVigente(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            return Optional.empty();
        }
        return buscarAbierta(idProducto).map(VigenciaPrecio::getPrecio);
    }

    /** Historial de precios del producto, del más nuevo al más viejo. */
    public List<VigenciaPrecioResponse> listarVigenciaPrecioActivo(String idProducto) {
        Producto producto = productoActivo(idProducto);
        return mapper.aRespuestas(vigencias.findByProducto_IdAndEliminadoFalseOrderByFechaDesdeDesc(producto.getId()));
    }

    private Optional<VigenciaPrecio> buscarAbierta(String idProducto) {
        return vigencias.findFirstByProducto_IdAndFechaHastaIsNullAndEliminadoFalseOrderByFechaDesdeDesc(idProducto);
    }

    private Producto productoActivo(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            throw new RecursoNoEncontradoException(PRODUCTO_NO_ENCONTRADO);
        }
        return productos.findByIdAndEliminadoFalse(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException(PRODUCTO_NO_ENCONTRADO));
    }

    private Producto productoParaActualizar(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            throw new RecursoNoEncontradoException(PRODUCTO_NO_ENCONTRADO);
        }
        return productos.buscarActivoParaActualizar(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException(PRODUCTO_NO_ENCONTRADO));
    }
}
