package com.zero.ecommerce.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CuitUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = { "30-71567890-6", "30715678906", " 20-12345678-6 ", "30-71234567-1" })
    void aceptaCuitsConDigitoVerificadorCorrecto(String cuit) {
        assertThat(CuitUtils.esValido(cuit)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "30-71567890-5", "3071567890", "307156789061", "30-7156789-06", "AB-71567890-6",
            "30.71567890.6" })
    void rechazaCuitsInvalidos(String cuit) {
        assertThat(CuitUtils.esValido(cuit)).isFalse();
    }

    @Test
    void normalizaConGuiones() {
        assertThat(CuitUtils.normalizar("30715678906")).isEqualTo("30-71567890-6");
        assertThat(CuitUtils.normalizar(" 30-71567890-6 ")).isEqualTo("30-71567890-6");
    }
}
