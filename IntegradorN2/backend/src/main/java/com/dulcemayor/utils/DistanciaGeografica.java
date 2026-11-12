package com.dulcemayor.utils;

/**
 * E5-01: distancia geográfica entre dos coordenadas con la fórmula de Haversine.
 *
 * NOTA (E5-01): es la distancia en línea recta sobre la esfera terrestre, no la distancia por calle.
 * Alcanza para elegir el punto más cercano; si en el futuro se necesita el recorrido real hay que
 * sumar un servicio de ruteo (por ejemplo OSRM) detrás de un Adapter.
 */
public final class DistanciaGeografica {

    /** Radio medio de la Tierra en kilómetros. */
    public static final double RADIO_TIERRA_KM = 6371.0088;

    private DistanciaGeografica() {
    }

    /** Distancia en kilómetros entre (latitud1, longitud1) y (latitud2, longitud2), en grados decimales. */
    public static double kilometros(double latitud1, double longitud1, double latitud2, double longitud2) {
        double dLatitud = Math.toRadians(latitud2 - latitud1);
        double dLongitud = Math.toRadians(longitud2 - longitud1);
        double a = Math.sin(dLatitud / 2) * Math.sin(dLatitud / 2)
                + Math.cos(Math.toRadians(latitud1)) * Math.cos(Math.toRadians(latitud2))
                * Math.sin(dLongitud / 2) * Math.sin(dLongitud / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }

    /** true si la latitud y la longitud son coordenadas válidas en grados decimales. */
    public static boolean coordenadasValidas(Double latitud, Double longitud) {
        return latitud != null && longitud != null && Double.isFinite(latitud) && Double.isFinite(longitud)
                && latitud >= -90 && latitud <= 90 && longitud >= -180 && longitud <= 180;
    }
}
