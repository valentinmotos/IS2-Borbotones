package com.dulcemayor.service;

import com.dulcemayor.dto.EstadisticasResponse;
import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.DetalleFacturaRepository;
import com.dulcemayor.repository.PedidoRepository;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EstadisticasService {
    private static final Set<EstadoPedido> ESTADOS_FACTURADOS = Set.of(
            EstadoPedido.FACTURADO, EstadoPedido.EN_REPARTO, EstadoPedido.ENTREGADO);

    private final PedidoRepository pedidos;
    private final DetalleFacturaRepository detallesFactura;
    private final StockService stock;

    @Transactional(readOnly = true)
    public EstadisticasResponse consultar(int anio, Integer mes) {
        if (anio < 1 || anio > 9999) {
            throw new ErrorServiceException("Indicá un año entre 1 y 9999.", "anio");
        }
        if (mes != null && (mes < 1 || mes > 12)) {
            throw new ErrorServiceException("Indicá un mes entre 1 y 12.", "mes");
        }

        LocalDate desde = LocalDate.of(anio, mes == null ? 1 : mes, 1);
        LocalDate hastaExclusivo = desde.plusMonths(mes == null ? 12 : 1);
        return new EstadisticasResponse(
                pedidos.contarPorDiaYPreventor(desde.atStartOfDay(), hastaExclusivo.atStartOfDay(), ESTADOS_FACTURADOS),
                stock.listarStock(),
                detallesFactura.listarMasVendidos(desde, hastaExclusivo, EstadoFactura.ANULADA, PageRequest.of(0, 10)));
    }
}
