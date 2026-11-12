package com.dulcemayor.service;

/**
 * E4-04: evento que publica {@link FacturacionService} cada vez que emite una factura.
 *
 * NOTA (E4-04 → E4-07): punto de extensión para el correo de confirmación. E4-07 no necesita tocar
 * la facturación: alcanza con un bean que escuche el evento después del commit, por ejemplo
 * {@code @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) void alFacturar(FacturaEmitidaEvent e)},
 * y que use {@code EmailService.enviar(...)} (ya es @Async). Si la facturación se deshace, el evento no llega.
 * Para adjuntar el PDF usar {@code FacturaPdfService.generarFacturaPdf(facturaId)} (E4-04, etapa 2).
 */
public record FacturaEmitidaEvent(String facturaId, String pedidoId) {
}
