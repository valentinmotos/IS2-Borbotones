package com.zero.ecommerce.services;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.CompraClienteDTO;
import com.zero.ecommerce.dto.PedidoDTO;
import com.zero.ecommerce.entities.Cliente;
import com.zero.ecommerce.entities.FacturaCliente;
import com.zero.ecommerce.entities.OrdenCompra;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FacturaClienteRepository;
import com.zero.ecommerce.repositories.OrdenCompraRepository;
import com.zero.ecommerce.utils.TextoUtils;

/**
 * Pedidos de los clientes para el panel (E4-06). Un pedido es una OrdenCompra que ya salió del carrito: los carritos
 * abiertos (PENDIENTE_COMPLETAR) no se listan. También arma "Mis compras" del cliente (E4-04). Las transiciones de
 * estado las decide la propia orden (E4-01).
 */
@Service
@Transactional(readOnly = true)
public class OrdenCompraService {

    // Estados de un pedido, en el orden del flujo. PENDIENTE_COMPLETAR es el carrito y no es un pedido.
    private static final Map<EstadoOrdenCompra, String> ESTADOS_PEDIDO = estadosPedido();

    private final OrdenCompraRepository repository;
    private final FacturaClienteRepository facturaClienteRepository;

    public OrdenCompraService(OrdenCompraRepository repository, FacturaClienteRepository facturaClienteRepository) {
        this.repository = repository;
        this.facturaClienteRepository = facturaClienteRepository;
    }

    /** Todos los pedidos activos (sin los carritos abiertos), del más nuevo al más viejo. */
    public List<OrdenCompra> listarPedidoActivo() {
        return repository.findByEliminadoFalseOrderByFechaDesc().stream()
                .filter(o -> o.getEstadoOrdenCompra() != null
                        && o.getEstadoOrdenCompra() != EstadoOrdenCompra.PENDIENTE_COMPLETAR)
                .toList();
    }

    /**
     * Pedidos filtrados por estado, forma de pago, rango de fechas y cliente (parte del nombre, apellido o correo, sin
     * importar mayúsculas ni tildes). Cada filtro es opcional (null o vacío).
     */
    public List<PedidoDTO> listarPedido(EstadoOrdenCompra estado, String idFormaDePago, LocalDate desde,
            LocalDate hasta, String cliente) throws ErrorServiceException {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ErrorServiceException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        Map<String, FacturaCliente> facturas = facturasPorOrden();
        String textoCliente = cliente == null || cliente.isBlank() ? null : TextoUtils.normalizar(cliente);
        return listarPedidoActivo().stream()
                .filter(o -> estado == null || o.getEstadoOrdenCompra() == estado)
                .filter(o -> desde == null || (o.getFecha() != null && !o.getFecha().isBefore(desde)))
                .filter(o -> hasta == null || (o.getFecha() != null && !o.getFecha().isAfter(hasta)))
                .filter(o -> textoCliente == null || coincideCliente(o.getCliente(), textoCliente))
                .filter(o -> idFormaDePago == null || idFormaDePago.isBlank()
                        || tieneFormaDePago(facturas.get(o.getId()), idFormaDePago))
                .map(o -> armarFila(o, facturas.get(o.getId())))
                .toList();
    }

    /** Compras del cliente (RF21): todas sus órdenes menos el carrito abierto, de la más nueva a la más vieja. */
    public List<OrdenCompra> listarComprasCliente(String idCliente) {
        if (idCliente == null || idCliente.isBlank()) {
            return List.of();
        }
        return repository.findByCliente_IdAndEliminadoFalseOrderByFechaDesc(idCliente).stream()
                .filter(o -> o.getEstadoOrdenCompra() != null
                        && o.getEstadoOrdenCompra() != EstadoOrdenCompra.PENDIENTE_COMPLETAR)
                .toList();
    }

    /** Filas de "Mis compras": las compras del cliente con la forma de pago de su factura. */
    public List<CompraClienteDTO> listarFilaCompraCliente(String idCliente) {
        Map<String, FacturaCliente> facturas = facturasPorOrden();
        return listarComprasCliente(idCliente).stream()
                .map(o -> {
                    FacturaCliente factura = facturas.get(o.getId());
                    return new CompraClienteDTO(o.getId(), o.getIdentificadorCompra(), o.getFecha(),
                            o.getCantidadTotalItems(), o.getTotal(), describirFormaDePago(factura),
                            o.getEstadoOrdenCompra().name());
                })
                .toList();
    }

    /** true si la orden es del cliente: un cliente no puede ver ni anular compras de otro. */
    public boolean esCompraDelCliente(OrdenCompra orden, String idCliente) {
        return orden != null && orden.getCliente() != null && idCliente != null
                && idCliente.equals(orden.getCliente().getId());
    }

    /** Cantidad de pedidos activos en cada estado, en el orden del flujo. Los estados sin pedidos quedan en 0. */
    public Map<String, Long> contarPedidoPorEstado() {
        Map<String, Long> cantidades = new LinkedHashMap<>();
        ESTADOS_PEDIDO.keySet().forEach(e -> cantidades.put(e.name(), 0L));
        for (OrdenCompra orden : listarPedidoActivo()) {
            cantidades.computeIfPresent(orden.getEstadoOrdenCompra().name(), (e, cantidad) -> cantidad + 1);
        }
        return cantidades;
    }

    /** Un pedido activo. Un carrito abierto no es un pedido, así que tampoco se encuentra. */
    public OrdenCompra buscarPedido(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("El pedido no existe o fue eliminado.");
        }
        return repository.findById(id)
                .filter(o -> !o.isEliminado() && o.getEstadoOrdenCompra() != null
                        && o.getEstadoOrdenCompra() != EstadoOrdenCompra.PENDIENTE_COMPLETAR)
                .orElseThrow(() -> new ErrorServiceException("El pedido no existe o fue eliminado."));
    }

    /** Busca una orden activa por su identificadorCompra (ej: ORD-DEMO0001). */
    public Optional<OrdenCompra> buscarPorIdentificadorCompra(String identificadorCompra) {
        if (identificadorCompra == null || identificadorCompra.isBlank()) {
            return Optional.empty();
        }
        return repository.findByIdentificadorCompraAndEliminadoFalse(identificadorCompra);
    }

    /** Busca una orden activa por su identificadorCompra (ej: ORD-DEMO0001) o por su ID UUID. */
    public Optional<OrdenCompra> buscarOrdenPorIdOIdentificador(String idOIdentificador) {
        if (idOIdentificador == null || idOIdentificador.isBlank()) {
            return Optional.empty();
        }
        Optional<OrdenCompra> porIdentificador = repository.findByIdentificadorCompraAndEliminadoFalse(idOIdentificador);
        if (porIdentificador.isPresent()) {
            return porIdentificador;
        }
        return repository.findById(idOIdentificador).filter(o -> !o.isEliminado());
    }

    /** La factura del pedido, o vacío si todavía no tiene (por ejemplo, un pedido cargado antes del checkout). */
    public Optional<FacturaCliente> buscarFacturaDePedido(String idOrden) {
        return facturaClienteRepository.findFirstByOrdenCompra_IdAndEliminadoFalse(idOrden);
    }

    /** Estados para el filtro del listado y las tarjetas: nombre del enum → texto visible. */
    public Map<String, String> listarEstadoPedido() {
        Map<String, String> estados = new LinkedHashMap<>();
        ESTADOS_PEDIDO.forEach((estado, texto) -> estados.put(estado.name(), texto));
        return estados;
    }

    /** Convierte el filtro de estado recibido al enum. Vacío, desconocido o el del carrito no filtra. */
    public EstadoOrdenCompra convertirEstado(String estado) {
        for (EstadoOrdenCompra valor : ESTADOS_PEDIDO.keySet()) {
            if (valor.name().equals(estado)) {
                return valor;
            }
        }
        return null;
    }

    private Map<String, FacturaCliente> facturasPorOrden() {
        return facturaClienteRepository.findByOrdenCompraIsNotNullAndEliminadoFalse().stream()
                .collect(Collectors.toMap(f -> f.getOrdenCompra().getId(), Function.identity(), (a, b) -> a));
    }

    private PedidoDTO armarFila(OrdenCompra orden, FacturaCliente factura) {
        return new PedidoDTO(orden.getId(), orden.getIdentificadorCompra(), orden.getFecha(),
                describirCliente(orden.getCliente()), orden.getTotal(), describirFormaDePago(factura),
                orden.getEstadoOrdenCompra().name());
    }

    private String describirFormaDePago(FacturaCliente factura) {
        return factura != null && factura.getFormaDePago() != null ? factura.getFormaDePago().getObservacion() : null;
    }

    private boolean tieneFormaDePago(FacturaCliente factura, String idFormaDePago) {
        return factura != null && factura.getFormaDePago() != null
                && factura.getFormaDePago().getId().equals(idFormaDePago);
    }

    private boolean coincideCliente(Cliente cliente, String texto) {
        if (cliente == null) {
            return false;
        }
        String correo = cliente.getUsuario() != null ? cliente.getUsuario().getNombreUsuario() : "";
        return TextoUtils.normalizar(describirCliente(cliente) + " " + correo).contains(texto);
    }

    private String describirCliente(Cliente cliente) {
        if (cliente == null) {
            return "-";
        }
        return (nulo(cliente.getNombre()) + " " + nulo(cliente.getApellido())).strip();
    }

    private String nulo(String texto) {
        return texto == null ? "" : texto;
    }

    private static Map<EstadoOrdenCompra, String> estadosPedido() {
        Map<EstadoOrdenCompra, String> estados = new LinkedHashMap<>();
        estados.put(EstadoOrdenCompra.PENDIENTE_PAGO, "Pendiente de pago");
        estados.put(EstadoOrdenCompra.PENDIENTE_ENVIO, "Pendiente de envío");
        estados.put(EstadoOrdenCompra.PENDIENTE_ENTREGA, "Pendiente de entrega");
        estados.put(EstadoOrdenCompra.ENTREGADO, "Entregado");
        estados.put(EstadoOrdenCompra.ANULADA, "Anulada");
        return estados;
    }
}
