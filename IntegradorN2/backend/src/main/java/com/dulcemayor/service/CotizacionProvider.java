package com.dulcemayor.service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Patrón Adapter (E1-08) · participante: Target. Interfaz propia del dominio para obtener la
 * cotización del dólar sin depender de una API externa concreta. El adaptador (DolarApiAdapter)
 * traduce el JSON de DolarAPI (Adaptee) a CotizacionExterna.
 *
 * Contrato del kickoff: obtenerVigente(). Las pantallas (E3-02, E3-03) no lo llaman directo:
 * usan CotizacionDolarService.buscarCotizacionVigente(), que guarda, cachea y tiene respaldo.
 */
public interface CotizacionProvider {

    /** Cotización oficial (dólar vendedor) del día, o vacío si la API falla o no responde a tiempo. */
    Optional<CotizacionExterna> obtenerVigente();

    record CotizacionExterna(LocalDate fecha, double valorVenta) {
    }
}
