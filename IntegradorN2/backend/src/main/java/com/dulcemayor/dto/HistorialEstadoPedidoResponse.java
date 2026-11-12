package com.dulcemayor.dto;

import com.dulcemayor.entity.HistorialEstadoPedido;
import com.dulcemayor.entity.enums.EstadoPedido;
import java.time.LocalDateTime;

public record HistorialEstadoPedidoResponse(
        EstadoPedido estado, LocalDateTime fechaHora, String usuarioId, String usuario,
        String observacion, Transportista transportista) {

    public static HistorialEstadoPedidoResponse desde(HistorialEstadoPedido registro) {
        var distribuidor = registro.getTransportista();
        Transportista transportista = distribuidor == null ? null
                : new Transportista(distribuidor.getId(), distribuidor.getNombre(), distribuidor.getApellido());
        return new HistorialEstadoPedidoResponse(registro.getEstado(), registro.getFechaHora(),
                registro.getUsuario().getId(), registro.getUsuario().getNombreUsuario(),
                registro.getObservacion(), transportista);
    }

    public record Transportista(String id, String nombre, String apellido) {}
}
