package com.dulcemayor.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dulcemayor.service.CotizacionProvider;
import com.dulcemayor.service.CotizacionProvider.CotizacionExterna;

/**
 * E1-08 de punta a punta: el endpoint es público (sin token) y la segunda consulta sale de la
 * caché sin volver a llamar a la API. DolarAPI se reemplaza por un mock (los tests no salen a internet).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CotizacionDolarIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CotizacionProvider provider;

    @Test
    void cotizacionVigentePublicaYCacheada() throws Exception {
        when(provider.obtenerVigente())
                .thenReturn(Optional.of(new CotizacionExterna(LocalDate.of(2026, 11, 9), 1450)));

        mvc.perform(get("/api/publico/cotizacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2026-11-09"))
                .andExpect(jsonPath("$.valorVenta").value(1450.0));
        mvc.perform(get("/api/publico/cotizacion")).andExpect(status().isOk());

        verify(provider, times(1)).obtenerVigente();
    }
}
