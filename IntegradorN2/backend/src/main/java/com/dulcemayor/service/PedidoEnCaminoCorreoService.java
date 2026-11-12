package com.dulcemayor.service;

import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.PuntoDeVenta;
import com.dulcemayor.repository.PedidoRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * E5-02: correo "tu pedido está en camino" al próximo comercio de la ruta de reparto. Se manda recién
 * cuando se confirma la entrega anterior (si la transacción falla, no sale ningún aviso), igual que el
 * correo de factura de E4-07.
 */
@Service
public class PedidoEnCaminoCorreoService {
    static final String PLANTILLA = "correo/pedido-en-camino";

    private static final Logger log = LoggerFactory.getLogger(PedidoEnCaminoCorreoService.class);

    private final PedidoRepository pedidos;
    private final DestinatarioMinorista destinatarios;
    private final EmailService correo;

    public PedidoEnCaminoCorreoService(PedidoRepository pedidos, DestinatarioMinorista destinatarios,
            EmailService correo) {
        this.pedidos = pedidos;
        this.destinatarios = destinatarios;
        this.correo = correo;
    }

    @Async
    @TransactionalEventListener
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void alEstarEnCamino(PedidoEnCaminoEvent evento) {
        Pedido pedido = pedidos.findByIdAndEliminadoFalse(evento.pedidoId()).orElse(null);
        if (pedido == null) {
            log.warn("No se encontró el pedido {} para avisar que está en camino.", evento.pedidoId());
            return;
        }
        PuntoDeVenta punto = pedido.getPuntoDeVenta();
        Optional<String> destino = destinatarios.buscar(punto.getMinorista());
        if (destino.isEmpty()) {
            log.warn("No hay correo para avisarle al responsable del pedido {} que está en camino.", pedido.getId());
            return;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("numeroPedido", pedido.getNumeroPedido());
        variables.put("comercio", punto.getNombreComercio());
        variables.put("direccion", direccion(punto.getDireccion()));
        variables.put("distribuidor", evento.distribuidor());
        variables.put("total", pedido.getTotal());
        correo.enviar(destino.get(), "Tu pedido N° " + pedido.getNumeroPedido() + " está en camino · Dulce Mayor",
                PLANTILLA, variables);
    }

    private static String direccion(Direccion direccion) {
        if (direccion == null) {
            return "";
        }
        String calle = ((direccion.getCalle() == null ? "" : direccion.getCalle()) + " "
                + (direccion.getNumeracion() == null ? "" : direccion.getNumeracion())).strip();
        return direccion.getLocalidad() == null ? calle : calle + ", " + direccion.getLocalidad().getNombre();
    }
}
