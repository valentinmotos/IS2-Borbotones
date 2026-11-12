package com.dulcemayor.entity;

import com.dulcemayor.entity.enums.EstadoPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "paradas_ruta")
@Getter
@Setter
public class ParadaRuta extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "punto_de_venta_id", nullable = false)
    private PuntoDeVenta puntoDeVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Column(nullable = false)
    private int orden;

    /**
     * NOTA (E5-02): atributo agregado respecto del UML. Fecha en que se le avisó por correo al comercio
     * que su pedido está en camino; null si todavía no se avisó. Evita mandar el mismo aviso dos veces
     * cuando el distribuidor entrega fuera del orden sugerido y la parada vuelve a quedar como "siguiente".
     * Solo se usa en las rutas de reparto.
     */
    @Column(name = "fecha_aviso_en_camino")
    private LocalDateTime fechaAvisoEnCamino;

    /**
     * Si la parada todavía falta hacerla. El criterio depende del tipo de ruta:
     * <ul>
     *   <li>Ruta de preventor (E5-01): pendiente mientras no tenga pedido cargado.</li>
     *   <li>Ruta de reparto, con distribuidor (E5-02): pendiente mientras su pedido no esté ENTREGADO.</li>
     * </ul>
     */
    public boolean estaPendiente() {
        if (ruta != null && ruta.esDeReparto()) {
            return pedido != null && pedido.getEstado() != EstadoPedido.ENTREGADO;
        }
        return pedido == null;
    }
}
