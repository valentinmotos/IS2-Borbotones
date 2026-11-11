package com.dulcemayor.service.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.dulcemayor.exception.ServicioNoDisponibleException;
import com.dulcemayor.service.ChatbotProvider.ItemInterpretado;
import com.fasterxml.jackson.databind.ObjectMapper;

/** E3-04: traducción entre la API de Ollama y ChatbotProvider, sin un Ollama real. */
class OllamaChatbotAdapterTest {

    private static final List<String> CATALOGO = List.of("Alfajor de chocolate", "Chupetín frutal");

    private MockRestServiceServer servidor;
    private OllamaChatbotAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ollama:11434");
        servidor = MockRestServiceServer.bindTo(builder).build();
        adapter = new OllamaChatbotAdapter(builder.build(), "modelo-de-prueba", new ObjectMapper());
    }

    /** Respuesta de /api/chat cuyo mensaje del asistente es el JSON indicado (como texto). */
    private static String respuestaOllama(String contenido) throws Exception {
        return new ObjectMapper().writeValueAsString(java.util.Map.of(
                "model", "modelo-de-prueba", "done", true,
                "message", java.util.Map.of("role", "assistant", "content", contenido)));
    }

    @Test
    void enviaModeloFormatoYCatalogoYTraduceLosItems() throws Exception {
        servidor.expect(requestTo("http://ollama:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("modelo-de-prueba"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.format.properties.items.type").value("array"))
                .andExpect(jsonPath("$.options.temperature").value(0))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value(
                        org.hamcrest.Matchers.containsString("- Chupetín frutal")))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("quiero 10 alfajores y 5 chupetines"))
                .andRespond(withSuccess(respuestaOllama("""
                        {"items":[{"producto":"Alfajor de chocolate","cantidad":10},
                                  {"producto":" Chupetín frutal ","cantidad":5}]}"""),
                        MediaType.APPLICATION_JSON));

        List<ItemInterpretado> items = adapter.interpretar("quiero 10 alfajores y 5 chupetines", CATALOGO);

        assertThat(items).containsExactly(
                new ItemInterpretado("Alfajor de chocolate", 10),
                new ItemInterpretado("Chupetín frutal", 5));
        servidor.verify();
    }

    @Test
    void descartaRenglonesSinProductoOConCantidadInvalida() throws Exception {
        servidor.expect(requestTo("http://ollama:11434/api/chat"))
                .andRespond(withSuccess(respuestaOllama("""
                        {"items":[{"producto":"","cantidad":3},{"producto":"Chicle","cantidad":0},
                                  {"producto":"Gomitas","cantidad":-2},{"producto":"Chicle","cantidad":4}]}"""),
                        MediaType.APPLICATION_JSON));

        assertThat(adapter.interpretar("chicles", CATALOGO)).containsExactly(new ItemInterpretado("Chicle", 4));
    }

    @Test
    void sinItemsDevuelveListaVacia() throws Exception {
        servidor.expect(requestTo("http://ollama:11434/api/chat"))
                .andRespond(withSuccess(respuestaOllama("{\"items\":[]}"), MediaType.APPLICATION_JSON));

        assertThat(adapter.interpretar("hola", CATALOGO)).isEmpty();
    }

    @Test
    void siOllamaFallaLanzaServicioNoDisponible() {
        servidor.expect(requestTo("http://ollama:11434/api/chat")).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.interpretar("quiero alfajores", CATALOGO))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage(OllamaChatbotAdapter.MENSAJE_NO_DISPONIBLE);
    }

    @Test
    void siElModeloDevuelveTextoQueNoEsJsonLanzaServicioNoDisponible() throws Exception {
        servidor.expect(requestTo("http://ollama:11434/api/chat"))
                .andRespond(withSuccess(respuestaOllama("Claro, acá tenés tu presupuesto"),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.interpretar("quiero alfajores", CATALOGO))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void elTiempoDeEsperaVacioOInvalidoUsaElValorPorDefecto() {
        assertThat(OllamaChatbotAdapter.timeout("")).isEqualTo(OllamaChatbotAdapter.TIMEOUT_POR_DEFECTO_MS);
        assertThat(OllamaChatbotAdapter.timeout(null)).isEqualTo(OllamaChatbotAdapter.TIMEOUT_POR_DEFECTO_MS);
        assertThat(OllamaChatbotAdapter.timeout("abc")).isEqualTo(OllamaChatbotAdapter.TIMEOUT_POR_DEFECTO_MS);
        assertThat(OllamaChatbotAdapter.timeout("0")).isEqualTo(OllamaChatbotAdapter.TIMEOUT_POR_DEFECTO_MS);
        assertThat(OllamaChatbotAdapter.timeout(" 90000 ")).isEqualTo(90_000);
    }

    @Test
    void sinModeloConfiguradoUsaElModeloPorDefecto() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ollama:11434");
        MockRestServiceServer otro = MockRestServiceServer.bindTo(builder).build();
        OllamaChatbotAdapter sinModelo = new OllamaChatbotAdapter(builder.build(), "  ", new ObjectMapper());
        otro.expect(jsonPath("$.model").value(OllamaChatbotAdapter.MODELO_POR_DEFECTO))
                .andRespond(withSuccess(respuestaOllama("{\"items\":[]}"), MediaType.APPLICATION_JSON));

        sinModelo.interpretar("hola", List.of());

        otro.verify();
    }
}
