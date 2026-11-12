package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.exception.ErrorServiceException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pedidos", uniqueConstraints = @UniqueConstraint(
        name = "uk_pedidos_preventor_clave_carga", columnNames = {"preventor_id", "clave_carga"}))
@Getter
@Setter
public class Pedido extends BaseEntity {
    @Column(name = "numero_pedido", nullable = false, unique = true)
    private long numeroPedido;

    @Column(name = "fecha_hora_carga", nullable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime fechaHoraCarga;

    @Column(nullable = false)
    private double subtotal;

    @Column(nullable = false)
    private double descuento;

    @Column(nullable = false)
    private double total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter(AccessLevel.NONE)
    private EstadoPedido estado;

    @Column(name = "codigo_promocion", length = 50)
    private String codigoPromocion;

    @Column(name = "clave_carga", length = 36, updatable = false)
    private String claveCarga;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "punto_de_venta_id", nullable = false)
    private PuntoDeVenta puntoDeVenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preventor_id", nullable = false)
    private Preventor preventor;

    /** Composición Pedido 1..n DetallePedido: el detalle se guarda y se borra junto con el pedido. */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    /** Agrega una línea manteniendo las dos puntas de la relación. */
    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
    }

    public HistorialEstadoPedido registrarCarga(Usuario usuario, LocalDateTime fecha, String observacion) {
        if (estado != null || getId() != null) {
            throw new ErrorServiceException("El pedido ya fue cargado.", "estado");
        }
        HistorialEstadoPedido registro = registrarEstado(
                EstadoPedido.CARGADO, usuario, fecha, observacion, "Pedido cargado.", null);
        fechaHoraCarga = fecha;
        return registro;
    }

    public HistorialEstadoPedido avanzar(EventoPedido evento, Usuario usuario, LocalDateTime fecha,
            String observacion, Distribuidor transportista) {
        if (estado == null) {
            throw new ErrorServiceException("Primero hay que registrar la carga del pedido.", "estado");
        }
        EstadoPedido siguiente = estado.avanzar(evento);
        if ((siguiente == EstadoPedido.EN_REPARTO || siguiente == EstadoPedido.ENTREGADO) && transportista == null) {
            throw new ErrorServiceException("Indicá el transportista del pedido.", "transportistaId");
        }
        if (siguiente != EstadoPedido.EN_REPARTO && siguiente != EstadoPedido.ENTREGADO && transportista != null) {
            throw new ErrorServiceException("El transportista solo corresponde al reparto y la entrega.", "transportistaId");
        }
        return registrarEstado(siguiente, usuario, fecha, observacion, evento.observacion(), transportista);
    }

    private HistorialEstadoPedido registrarEstado(EstadoPedido siguiente, Usuario usuario, LocalDateTime fecha,
            String observacion, String observacionPorDefecto, Distribuidor transportista) {
        if (usuario == null) {
            throw new ErrorServiceException("Indicá el usuario responsable del cambio.", "usuarioId");
        }
        if (fecha == null) {
            throw new ErrorServiceException("Indicá la fecha del cambio de estado.", "fechaHora");
        }
        String texto = observacion == null || observacion.isBlank() ? observacionPorDefecto : observacion.strip();
        if (texto.length() > 500) {
            throw new ErrorServiceException("La observación admite hasta 500 caracteres.", "observacion");
        }
        HistorialEstadoPedido registro = new HistorialEstadoPedido();
        registro.setPedido(this);
        registro.setEstado(siguiente);
        registro.setFechaHora(fecha);
        registro.setUsuario(usuario);
        registro.setObservacion(texto);
        registro.setTransportista(transportista);
        estado = siguiente;
        return registro;
    }
}
