package com.dulcemayor.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class DistanciaGeograficaTest {

    @Test
    void unGradoDeLatitudSonUnos111Kilometros() {
        assertThat(DistanciaGeografica.kilometros(0, 0, 1, 0)).isCloseTo(111.19, within(0.05));
    }

    @Test
    void laDistanciaAUnMismoPuntoEsCeroYEsSimetrica() {
        assertThat(DistanciaGeografica.kilometros(-32.8878, -68.8421, -32.8878, -68.8421)).isZero();
        assertThat(DistanciaGeografica.kilometros(-32.8878, -68.8421, -32.9260, -68.8440))
                .isEqualTo(DistanciaGeografica.kilometros(-32.9260, -68.8440, -32.8878, -68.8421));
    }

    @Test
    void mendozaCentroAGodoyCruzEstaAUnosCuatroKilometros() {
        assertThat(DistanciaGeografica.kilometros(-32.8878, -68.8421, -32.9260, -68.8440))
                .isCloseTo(4.25, within(0.1));
    }

    @Test
    void validaLosRangosDeLasCoordenadas() {
        assertThat(DistanciaGeografica.coordenadasValidas(-32.88, -68.84)).isTrue();
        assertThat(DistanciaGeografica.coordenadasValidas(null, -68.84)).isFalse();
        assertThat(DistanciaGeografica.coordenadasValidas(91.0, 0.0)).isFalse();
        assertThat(DistanciaGeografica.coordenadasValidas(0.0, -181.0)).isFalse();
        assertThat(DistanciaGeografica.coordenadasValidas(Double.NaN, 0.0)).isFalse();
    }
}
