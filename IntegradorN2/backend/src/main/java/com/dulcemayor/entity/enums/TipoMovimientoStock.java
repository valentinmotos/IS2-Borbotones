package com.dulcemayor.entity.enums;

/** E2-02: origen de un movimiento de stock. */
public enum TipoMovimientoStock {
    /** Ingreso de mercadería (reposición). */
    INGRESO,
    /** Corrección manual del saldo, en más o en menos. */
    AJUSTE,
    /** Salida por venta (la usa la facturación, E4-04). */
    EGRESO
}
