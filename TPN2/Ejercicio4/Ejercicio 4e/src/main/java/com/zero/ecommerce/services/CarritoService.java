package com.zero.ecommerce.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.DetalleCompra;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ClienteRepository;
import com.zero.ecommerce.repositories.OrdenCompraRepository;
import com.zero.ecommerce.repositories.ProductoRepository;

/**
 * Servicio para la gestión del carrito de compras del cliente (RF17).
 * El carrito es la {@link OrdenCompra} del cliente en estado {@link EstadoOrdenCompra#PENDIENTE_COMPLETAR}.
 * Implementa los patrones Creador (OrdenCompra crea DetalleCompra) y Experto (DetalleCompra y OrdenCompra
 * calculan subtotales y totales).
 */
@Service
@Transactional
public class CarritoService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final StockService stockService;
    private final VigenciaPrecioService vigenciaPrecioService;

    public CarritoService(OrdenCompraRepository ordenCompraRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            StockService stockService,
            VigenciaPrecioService vigenciaPrecioService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.stockService = stockService;
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    /**
     * Obtiene la orden abierta (carrito) del cliente en PENDIENTE_COMPLETAR o crea una si aún no existe.
     */
    public OrdenCompra obtenerCarrito(String idCliente) throws ErrorServiceException {
        validarCliente(idCliente);
        Cliente cliente = clienteRepository.findByIdAndEliminadoFalse(idCliente)
                .orElseThrow(() -> new ErrorServiceException("El cliente no existe o fue eliminado."));

        return ordenCompraRepository
                .findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                        idCliente, EstadoOrdenCompra.PENDIENTE_COMPLETAR)
                .orElseGet(() -> crearNuevoCarrito(cliente));
    }

    /**
     * Revalida stock y precios del carrito abierto al abrir la página.
     * Si un producto ya no tiene stock suficiente, se ajusta la cantidad y se avisa.
     * Si se quedó sin stock o sin precio vigente, se retira y se notifica.
     *
     * @return Lista de mensajes descriptivos para mostrarle al cliente sobre los ajustes realizados.
     */
    public List<String> sincronizarAjustes(String idCliente) throws ErrorServiceException {
        OrdenCompra orden = obtenerCarrito(idCliente);
        List<String> avisos = new ArrayList<>();
        boolean huboCambios = false;

        Iterator<DetalleCompra> iterator = orden.getDetalles().iterator();
        while (iterator.hasNext()) {
            DetalleCompra detalle = iterator.next();
            if (detalle.isEliminado()) {
                continue;
            }

            Producto producto = detalle.getProducto();
            if (producto == null || producto.isEliminado()) {
                iterator.remove();
                avisos.add("Un producto que tenías en el carrito ya no está disponible y fue retirado.");
                huboCambios = true;
                continue;
            }

            double precioVigente;
            try {
                precioVigente = vigenciaPrecioService.buscarPrecioVigente(producto.getId());
            } catch (ErrorServiceException e) {
                iterator.remove();
                avisos.add("El producto \"" + producto.getNombre()
                        + "\" no tiene precio vigente actualmente y fue retirado de tu carrito.");
                huboCambios = true;
                continue;
            }

            int stockActual = stockService.buscarStockActual(producto.getId());
            if (stockActual <= 0) {
                iterator.remove();
                avisos.add("El producto \"" + producto.getNombre()
                        + "\" se quedó sin stock disponible y fue retirado de tu carrito.");
                huboCambios = true;
            } else if (detalle.getCantidad() > stockActual) {
                int cantidadAnterior = detalle.getCantidad();
                detalle.setCantidad(stockActual);
                detalle.calcularSubtotal(precioVigente);
                avisos.add("El producto \"" + producto.getNombre() + "\" (Talle: " + producto.getTalle()
                        + ") tenía " + cantidadAnterior + " unidades, pero actualmente hay " + stockActual
                        + " disponibles. Se ajustó la cantidad en tu carrito.");
                huboCambios = true;
            } else {
                // Actualizamos subtotal si cambió el precio vigente
                double subtotalEsperado = detalle.getCantidad() * precioVigente;
                if (Double.compare(detalle.getSubtotal(), subtotalEsperado) != 0) {
                    detalle.calcularSubtotal(precioVigente);
                    huboCambios = true;
                }
            }
        }

        if (huboCambios) {
            orden.recalcularTotal();
            ordenCompraRepository.save(orden);
        }

        return avisos;
    }

    /**
     * Agrega un producto al carrito. Si el producto ya está en el carrito, suma la cantidad.
     * Valida que el producto esté activo, con precio vigente y que la cantidad no supere el stock actual.
     */
    public DetalleCompra agregarProducto(String idCliente, String idProducto, int cantidad)
            throws ErrorServiceException {
        if (cantidad <= 0) {
            throw new ErrorServiceException("La cantidad a agregar debe ser mayor a 0.");
        }
        validarCliente(idCliente);
        if (idProducto == null || idProducto.isBlank()) {
            throw new ErrorServiceException("El producto es obligatorio.");
        }

        Producto producto = productoRepository.findByIdAndEliminadoFalse(idProducto)
                .orElseThrow(() -> new ErrorServiceException("El producto no existe o fue eliminado."));

        double precioVigente = vigenciaPrecioService.buscarPrecioVigente(idProducto);

        int stockActual = stockService.buscarStockActual(idProducto);
        if (stockActual <= 0) {
            throw new ErrorServiceException("El producto \"" + producto.getNombre() + "\" no cuenta con stock disponible.");
        }

        OrdenCompra orden = obtenerCarrito(idCliente);
        Optional<DetalleCompra> detalleExistente = orden.buscarDetallePorProducto(idProducto);

        int cantidadFinal = cantidad;
        if (detalleExistente.isPresent()) {
            cantidadFinal += detalleExistente.get().getCantidad();
        }

        if (cantidadFinal > stockActual) {
            String mensaje = "No hay suficiente stock para agregar esa cantidad. Stock disponible: " + stockActual;
            if (detalleExistente.isPresent()) {
                mensaje += " (ya tenés " + detalleExistente.get().getCantidad() + " en tu carrito).";
            }
            throw new ErrorServiceException(mensaje);
        }

        DetalleCompra detalle;
        if (detalleExistente.isPresent()) {
            detalle = detalleExistente.get();
            detalle.setCantidad(cantidadFinal);
            detalle.calcularSubtotal(precioVigente); // Patrón Experto
            orden.recalcularTotal(); // Patrón Experto
        } else {
            // Patrón Creador: la orden crea su propio DetalleCompra
            detalle = orden.crearDetalle(producto, cantidad, precioVigente);
        }

        ordenCompraRepository.save(orden);
        return detalle;
    }

    /**
     * Modifica la cantidad de un ítem existente en el carrito.
     */
    public void modificarCantidad(String idCliente, String idDetalle, int nuevaCantidad)
            throws ErrorServiceException {
        if (nuevaCantidad <= 0) {
            throw new ErrorServiceException("La cantidad debe ser al menos 1. Si deseás quitar el producto, usá el botón eliminar.");
        }
        if (idDetalle == null || idDetalle.isBlank()) {
            throw new ErrorServiceException("El ítem a modificar es obligatorio.");
        }

        OrdenCompra orden = obtenerCarrito(idCliente);
        DetalleCompra detalle = orden.getDetalles().stream()
                .filter(d -> !d.isEliminado() && idDetalle.equals(d.getId()))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("El producto no se encuentra en el carrito."));

        Producto producto = detalle.getProducto();
        if (producto == null || producto.isEliminado()) {
            orden.getDetalles().remove(detalle);
            orden.recalcularTotal();
            ordenCompraRepository.save(orden);
            throw new ErrorServiceException("El producto ya no está disponible.");
        }

        int stockActual = stockService.buscarStockActual(producto.getId());
        if (nuevaCantidad > stockActual) {
            throw new ErrorServiceException("No hay stock suficiente para esa cantidad. Stock disponible: " + stockActual);
        }

        double precioVigente = vigenciaPrecioService.buscarPrecioVigente(producto.getId());
        detalle.setCantidad(nuevaCantidad);
        detalle.calcularSubtotal(precioVigente); // Patrón Experto
        orden.recalcularTotal(); // Patrón Experto

        ordenCompraRepository.save(orden);
    }

    /**
     * Quita un ítem del carrito.
     */
    public void quitarProducto(String idCliente, String idDetalle) throws ErrorServiceException {
        if (idDetalle == null || idDetalle.isBlank()) {
            throw new ErrorServiceException("El ítem a quitar es obligatorio.");
        }

        OrdenCompra orden = obtenerCarrito(idCliente);
        DetalleCompra detalle = orden.getDetalles().stream()
                .filter(d -> !d.isEliminado() && idDetalle.equals(d.getId()))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("El producto no se encuentra en el carrito."));

        orden.getDetalles().remove(detalle);
        orden.recalcularTotal();
        ordenCompraRepository.save(orden);
    }

    /**
     * Vacía todos los ítems del carrito.
     */
    public void vaciarCarrito(String idCliente) throws ErrorServiceException {
        OrdenCompra orden = obtenerCarrito(idCliente);
        orden.getDetalles().clear();
        orden.recalcularTotal();
        ordenCompraRepository.save(orden);
    }

    /**
     * Devuelve la cantidad de ítems del carrito abierto, o 0 si no existe.
     */
    @Transactional(readOnly = true)
    public int contarItemsCarrito(String idCliente) {
        if (idCliente == null || idCliente.isBlank()) {
            return 0;
        }
        return ordenCompraRepository
                .findFirstByCliente_IdAndEstadoOrdenCompraAndEliminadoFalseOrderByFechaDesc(
                        idCliente, EstadoOrdenCompra.PENDIENTE_COMPLETAR)
                .map(OrdenCompra::getCantidadTotalItems)
                .orElse(0);
    }

    /**
     * Obtiene el Cliente asociado al Usuario dado, o crea una entidad Cliente inicial si aún no fue creada.
     */
    public Cliente obtenerOCrearClienteParaUsuario(Usuario usuario) throws ErrorServiceException {
        if (usuario == null) {
            throw new ErrorServiceException("El usuario es obligatorio.");
        }
        return clienteRepository.findByUsuario_IdAndEliminadoFalse(usuario.getId())
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setUsuario(usuario);
                    return clienteRepository.save(nuevo);
                });
    }

    private OrdenCompra crearNuevoCarrito(Cliente cliente) {
        OrdenCompra nueva = new OrdenCompra();
        nueva.setCliente(cliente);
        nueva.setFecha(LocalDate.now());
        nueva.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
        nueva.setIdentificadorCompra("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        nueva.setTotal(0.0);
        return ordenCompraRepository.save(nueva);
    }

    private void validarCliente(String idCliente) throws ErrorServiceException {
        if (idCliente == null || idCliente.isBlank()) {
            throw new ErrorServiceException("El cliente es obligatorio.");
        }
        if (!clienteRepository.existsByIdAndEliminadoFalse(idCliente)) {
            throw new ErrorServiceException("El cliente no existe o fue eliminado.");
        }
    }
}
