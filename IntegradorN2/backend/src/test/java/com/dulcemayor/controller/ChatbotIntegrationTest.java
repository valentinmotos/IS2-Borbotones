package com.dulcemayor.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dulcemayor.exception.ServicioNoDisponibleException;
import com.dulcemayor.service.ChatbotProvider;
import com.dulcemayor.service.ChatbotProvider.ItemInterpretado;
import com.dulcemayor.service.CotizacionProvider;
import com.dulcemayor.service.CotizacionProvider.CotizacionExterna;

/**
 * E3-04 de punta a punta con los productos del DataSeeder: endpoint público, validación contra el
 * catálogo real (H2) y respuesta 503 si la IA no está. Ollama y DolarAPI se reemplazan por mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatbotIntegrationTest {

    private static final String URL = "/api/publico/chatbot/presupuesto";
    private static final String PEDIDO = "{\"mensaje\":\"quiero 10 alfajores y 5 chupetines\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatbotProvider chatbot;

    @MockitoBean
    private CotizacionProvider cotizacion;

    @BeforeEach
    void setUp() {
        when(cotizacion.obtenerVigente())
                .thenReturn(Optional.of(new CotizacionExterna(LocalDate.now(), 1500)));
    }

    @Test
    void criterioDeAceptacionSinTokenConTotalCorrectoYSinElProductoSinStock() throws Exception {
        // Seeder: «Alfajor de chocolate» $6000 con stock 50; «Chupetín frutal» sin stock
        when(chatbot.interpretar(eq("quiero 10 alfajores y 5 chupetines"), anyList())).thenReturn(List.of(
                new ItemInterpretado("Alfajor de chocolate", 10), new ItemInterpretado("Chupetín frutal", 5)));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].codigo").value("ALF-001"))
                .andExpect(jsonPath("$.items[0].cantidad").value(10))
                .andExpect(jsonPath("$.items[0].precioUnitario").value(6000.0))
                .andExpect(jsonPath("$.items[0].subtotal").value(60000.0))
                .andExpect(jsonPath("$.excluidos.length()").value(1))
                .andExpect(jsonPath("$.excluidos[0].producto").value("Chupetín frutal"))
                .andExpect(jsonPath("$.excluidos[0].motivo").value("Sin stock."))
                .andExpect(jsonPath("$.total").value(60000.0))
                .andExpect(jsonPath("$.cotizacionDolar").value(1500.0))
                .andExpect(jsonPath("$.totalDolares").value(40.0));
    }

    @Test
    void mensajeVacioDevuelve400SinConsultarALaIa() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"mensaje\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("mensaje"))
                .andExpect(jsonPath("$.mensaje").value("Escribí qué productos querés presupuestar."));
        verifyNoInteractions(chatbot);
    }

    @Test
    void mensajeDemasiadoLargoDevuelve400() throws Exception {
        String largo = "a".repeat(501);
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"mensaje\":\"" + largo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("mensaje"));
    }

    @Test
    void siLaIaNoEstaDisponibleDevuelve503ConMensaje() throws Exception {
        when(chatbot.interpretar(anyString(), anyList()))
                .thenThrow(new ServicioNoDisponibleException("El asistente no está disponible en este momento."));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("El asistente no está disponible en este momento."));
    }
}
