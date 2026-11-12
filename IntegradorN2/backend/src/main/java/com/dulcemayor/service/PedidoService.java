package com.dulcemayor.service;

import com.dulcemayor.dto.HistorialEstadoPedidoResponse;
import com.dulcemayor.entity.Distribuidor;
import com.dulcemayor.entity.HistorialEstadoPedido;
import com.dulcemayor.entity.Pedido;
import com.dulcemayor.entity.Usuario;
import com.dulcemayor.entity.enums.EventoPedido;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.DistribuidorRepository;
import com.dulcemayor.repository.HistorialEstadoPedidoRepository;
import com.dulcemayor.repository.PedidoRepository;
import com.dulcemayor.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PedidoService {
    private static final String PEDIDO_NO_ENCONTRADO = "No se encontró el pedido.";

    private final PedidoRepository pedidos;
    private final HistorialEstadoPedidoRepository historial;
    private final UsuarioRepository usuarios;
    private final DistribuidorRepository distribuidores;
    private final PromocionService promociones;

    public PedidoService(PedidoRepository pedidos, HistorialEstadoPedidoRepository historial,
            UsuarioRepository usuarios, DistribuidorRepository distribuidores, PromocionService promociones) {
        this.pedidos = pedidos;
        this.historial = historial;
        this.usuarios = usuarios;
        this.distribuidores = distribuidores;
        this.promociones = promociones;
    }

    /** Persiste el pedido armado por el flujo de carga y su primer registro en la misma transacción. */
    @Transactional
    public Pedido registrarCarga(Pedido pedido, String usuarioId, String observacion) {
        promociones.aplicarPromocion(pedido, pedido.getCodigoPromocion());
        HistorialEstadoPedido registro = pedido.registrarCarga(
                buscarUsuario(usuarioId), LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS), observacion);
        Pedido guardado = pedidos.save(pedido);
        historial.save(registro);
        return guardado;
    }

    /** Registra el resultado de una operación de negocio dentro de su transacción. */
    @Transactional
    public HistorialEstadoPedidoResponse avanzar(String pedidoId, EventoPedido evento, String usuarioId,
            String observacion, String transportistaId) {
        Pedido pedido = pedidos.buscarActivoParaActualizar(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(PEDIDO_NO_ENCONTRADO));
        // El bloqueo mantiene la validación y el historial en orden ante cambios simultáneos.
        HistorialEstadoPedido anterior = historial
                .findFirstByPedido_IdAndEliminadoFalseOrderByFechaHoraDescIdDesc(pedidoId).orElse(null);
        Distribuidor transportista = resolverTransportista(evento, anterior, transportistaId);
        LocalDateTime fechaAnterior = anterior == null ? pedido.getFechaHoraCarga() : anterior.getFechaHora();
        LocalDateTime fecha = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        if (!fecha.isAfter(fechaAnterior)) {
            fecha = fechaAnterior.plus(1, ChronoUnit.MILLIS);
        }
        HistorialEstadoPedido registro = pedido.avanzar(
                evento, buscarUsuario(usuarioId), fecha, observacion, transportista);
        return HistorialEstadoPedidoResponse.desde(historial.save(registro));
    }

    public List<HistorialEstadoPedidoResponse> consultarHistorial(String pedidoId) {
        pedidos.findByIdAndEliminadoFalse(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(PEDIDO_NO_ENCONTRADO));
        return historial.findByPedido_IdAndEliminadoFalseOrderByFechaHoraAscIdAsc(pedidoId)
                .stream().map(HistorialEstadoPedidoResponse::desde).toList();
    }

    private Usuario buscarUsuario(String id) {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("Indicá el usuario responsable del cambio.", "usuarioId");
        }
        return usuarios.findByIdAndEliminadoFalse(id).filter(Usuario::isActivo)
                .orElseThrow(() -> new ErrorServiceException("El usuario responsable debe estar activo.", "usuarioId"));
    }

    private Distribuidor resolverTransportista(EventoPedido evento, HistorialEstadoPedido anterior, String id) {
        if (evento == EventoPedido.ENTREGAR && anterior != null && anterior.getTransportista() != null) {
            Distribuidor asignado = anterior.getTransportista();
            if (id != null && !id.isBlank() && !id.equals(asignado.getId())) {
                throw new ErrorServiceException("La entrega debe registrar al transportista del reparto.", "transportistaId");
            }
            return asignado;
        }
        if (id == null || id.isBlank()) {
            return null;
        }
        return distribuidores.findById(id).filter(distribuidor -> !distribuidor.isEliminado())
                .orElseThrow(() -> new ErrorServiceException("No se encontró el transportista activo.", "transportistaId"));
    }
}
