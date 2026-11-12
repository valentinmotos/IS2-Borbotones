package com.dulcemayor.dto;

import com.dulcemayor.entity.enums.EstadoFactura;
import com.dulcemayor.entity.enums.EstadoPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * E4-08: un pedido del minorista autenticado, con su factura si ya se emitió.
 *
 * <p>Los campos de factura ({@code facturaId}, {@code numeroFactura}, {@code fechaFactura},
 * {@code estadoFactura}, {@code saldoPendiente}) son null mientras el pedido no se facturó.
 * {@code numeroFactura} es el número visible con ceros (p. ej. "00000012").
 *
 * <p>NOTA (E4-08): {@code puedePagar} es true cuando la factura está PENDIENTE_PAGO y tiene saldo.
 * El plan habla de "pendientes de pago con billetera", pero el pedido no guarda la forma de pago
 * elegida (ni en el UML ni en el código), así que el botón «Pagar ahora» (Checkout Pro de E4-05)
 * se ofrece para toda factura impaga. Si un issue futuro agrega la forma de pago al pedido,
 * filtrar acá por BILLETERA_VIRTUAL.
 */
public record CompraMinoristaResponse(String pedidoId, long numeroPedido, LocalDateTime fechaHoraCarga,
        String comercio, double subtotal, double descuento, double total, EstadoPedido estadoPedido,
        String facturaId, String numeroFactura, LocalDate fechaFactura, EstadoFactura estadoFactura,
        BigDecimal saldoPendiente, boolean puedePagar) {
}
