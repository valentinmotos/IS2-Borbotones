package com.zero.ecommerce.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.zero.ecommerce.dto.PasoSeguimientoDTO;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

/**
 * Carrito del cliente. La venta es la FacturaCliente que se crea al confirmarlo.
 */
@Entity
@Getter
@Setter
public class OrdenCompra extends BaseEntity {

    private String identificadorCompra;

    private LocalDate fecha;

    private double total;

    @Enumerated(EnumType.STRING)
    private EstadoOrdenCompra estadoOrdenCompra;

    @ManyToOne
    private Cliente cliente;

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCompra> detalles = new ArrayList<>();

    /**
     * Creador: la orden crea sus detalles. El subtotal es cantidad * precio unitario; el precio unitario no se
     * guarda aparte porque el diagrama no lo tiene (se recalcula con {@link DetalleCompra#getPrecioUnitario()}).
     */
    public DetalleCompra crearDetalle(Producto producto, int cantidad, double precioUnitario) {
        DetalleCompra detalle = new DetalleCompra();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.calcularSubtotal(precioUnitario);
        detalle.setOrdenCompra(this);
        this.detalles.add(detalle);
        recalcularTotal();
        return detalle;
    }

    /** Experto: la orden sabe calcular su total como la suma de los subtotales de sus detalles activos. */
    public double recalcularTotal() {
        this.total = this.detalles.stream()
                .filter(d -> !d.isEliminado())
                .mapToDouble(DetalleCompra::getSubtotal)
                .sum();
        return this.total;
    }

    /** Busca si ya existe un ítem en el carrito para el producto dado. */
    public Optional<DetalleCompra> buscarDetallePorProducto(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            return Optional.empty();
        }
        return this.detalles.stream()
                .filter(d -> !d.isEliminado() && d.getProducto() != null && idProducto.equals(d.getProducto().getId()))
                .findFirst();
    }

    /** Cantidad total de unidades de productos en el carrito. */
    public int getCantidadTotalItems() {
        return this.detalles.stream()
                .filter(d -> !d.isEliminado())
                .mapToInt(DetalleCompra::getCantidad)
                .sum();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Transiciones de estado (E4-01). Experto: la orden sabe a qué estado puede pasar. Flujo lineal:
    // PENDIENTE_COMPLETAR -> PENDIENTE_PAGO -> PENDIENTE_ENVIO -> PENDIENTE_ENTREGA -> ENTREGADO, y ANULADA según
    // puedeAnularse(esAdmin). Cada método valida el estado de origen y, si no corresponde, lanza la excepción.
    // ---------------------------------------------------------------------------------------------------------------

    /** Nombres de los pasos de la línea de tiempo, en orden. "Pago realizado" no es un estado de la orden. */
    private static final List<String> PASOS_SEGUIMIENTO = List.of(
            "Pendiente de pago", "Pago realizado", "Pendiente de envío", "Pendiente de entrega", "Entregado");

    /** Confirma el carrito: pasa de PENDIENTE_COMPLETAR a PENDIENTE_PAGO. */
    public void confirmar() throws ErrorServiceException {
        validarNoAnulada();
        if (this.estadoOrdenCompra != EstadoOrdenCompra.PENDIENTE_COMPLETAR) {
            throw new ErrorServiceException("La orden ya fue confirmada.");
        }
        if (getCantidadTotalItems() == 0) {
            throw new ErrorServiceException("No se puede confirmar un carrito vacío.");
        }
        this.estadoOrdenCompra = EstadoOrdenCompra.PENDIENTE_PAGO;
    }

    /** Registra el pago: pasa de PENDIENTE_PAGO a PENDIENTE_ENVIO. */
    public void registrarPago() throws ErrorServiceException {
        validarNoAnulada();
        if (this.estadoOrdenCompra == EstadoOrdenCompra.PENDIENTE_COMPLETAR) {
            throw new ErrorServiceException("No se puede pagar una orden sin confirmar.");
        }
        if (this.estadoOrdenCompra != EstadoOrdenCompra.PENDIENTE_PAGO) {
            throw new ErrorServiceException("La orden ya fue pagada.");
        }
        this.estadoOrdenCompra = EstadoOrdenCompra.PENDIENTE_ENVIO;
    }

    /** Marca la orden como enviada: pasa de PENDIENTE_ENVIO a PENDIENTE_ENTREGA. */
    public void marcarEnviado() throws ErrorServiceException {
        validarNoAnulada();
        if (this.estadoOrdenCompra == EstadoOrdenCompra.PENDIENTE_COMPLETAR
                || this.estadoOrdenCompra == EstadoOrdenCompra.PENDIENTE_PAGO) {
            throw new ErrorServiceException("No se puede enviar una orden sin pagar.");
        }
        if (this.estadoOrdenCompra != EstadoOrdenCompra.PENDIENTE_ENVIO) {
            throw new ErrorServiceException("La orden ya fue enviada.");
        }
        this.estadoOrdenCompra = EstadoOrdenCompra.PENDIENTE_ENTREGA;
    }

    /** Marca la orden como entregada: pasa de PENDIENTE_ENTREGA a ENTREGADO. */
    public void marcarEntregado() throws ErrorServiceException {
        validarNoAnulada();
        if (this.estadoOrdenCompra == EstadoOrdenCompra.ENTREGADO) {
            throw new ErrorServiceException("La orden ya fue entregada.");
        }
        if (this.estadoOrdenCompra != EstadoOrdenCompra.PENDIENTE_ENTREGA) {
            throw new ErrorServiceException("No se puede entregar una orden que no fue enviada.");
        }
        this.estadoOrdenCompra = EstadoOrdenCompra.ENTREGADO;
    }

    /**
     * Anula la orden si {@link #puedeAnularse(boolean)} lo permite. Solo cambia el estado: la factura y el stock los
     * resuelve VentaService.anularVenta (E4-03).
     */
    public void anular(boolean esAdmin) throws ErrorServiceException {
        validarNoAnulada();
        if (!puedeAnularse(esAdmin)) {
            if (!esAdmin && this.estadoOrdenCompra == EstadoOrdenCompra.PENDIENTE_ENVIO) {
                throw new ErrorServiceException(
                        "La orden ya fue pagada. Para anularla comunicate con la tienda.");
            }
            throw new ErrorServiceException("No se puede anular una orden que ya fue enviada.");
        }
        this.estadoOrdenCompra = EstadoOrdenCompra.ANULADA;
    }

    /**
     * El cliente puede anular en PENDIENTE_COMPLETAR y PENDIENTE_PAGO; el administrador, además, en PENDIENTE_ENVIO.
     */
    public boolean puedeAnularse(boolean esAdmin) {
        if (this.estadoOrdenCompra == null) {
            return false;
        }
        return switch (this.estadoOrdenCompra) {
            case PENDIENTE_COMPLETAR, PENDIENTE_PAGO -> true;
            case PENDIENTE_ENVIO -> esAdmin;
            default -> false;
        };
    }

    /**
     * Pasos de la línea de tiempo del seguimiento. Los pasos anteriores al estado actual quedan completos; en
     * ENTREGADO el último paso es a la vez completo y actual. Una orden en el carrito, anulada o sin estado devuelve
     * todos los pasos sin completar y sin paso actual (la vista muestra el badge del estado).
     */
    public List<PasoSeguimientoDTO> pasosSeguimiento() {
        int actual = indicePasoActual();
        boolean entregado = this.estadoOrdenCompra == EstadoOrdenCompra.ENTREGADO;
        List<PasoSeguimientoDTO> pasos = new ArrayList<>();
        for (int i = 0; i < PASOS_SEGUIMIENTO.size(); i++) {
            boolean completo = i < actual || (entregado && i == actual);
            pasos.add(new PasoSeguimientoDTO(PASOS_SEGUIMIENTO.get(i), completo, i == actual));
        }
        return pasos;
    }

    /** Posición del estado actual en PASOS_SEGUIMIENTO, o -1 si no está en la línea de tiempo. */
    private int indicePasoActual() {
        if (this.estadoOrdenCompra == null) {
            return -1;
        }
        return switch (this.estadoOrdenCompra) {
            case PENDIENTE_PAGO -> 0;
            case PENDIENTE_ENVIO -> 2;
            case PENDIENTE_ENTREGA -> 3;
            case ENTREGADO -> 4;
            default -> -1;
        };
    }

    private void validarNoAnulada() throws ErrorServiceException {
        if (this.estadoOrdenCompra == EstadoOrdenCompra.ANULADA) {
            throw new ErrorServiceException("La orden está anulada y no admite cambios de estado.");
        }
    }
}

