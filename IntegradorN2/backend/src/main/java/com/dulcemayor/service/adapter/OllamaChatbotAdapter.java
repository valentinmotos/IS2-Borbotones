package com.dulcemayor.service.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dulcemayor.exception.ServicioNoDisponibleException;
import com.dulcemayor.service.ChatbotProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Patrón Adapter (E3-04) · participante: Adapter. Adapta la API HTTP de Ollama (Adaptee,
 * POST {OLLAMA_URL}/api/chat) a la interfaz ChatbotProvider (Target).
 *
 * Le pide al modelo una respuesta con forma fija (parámetro "format" con un JSON Schema):
 * {"items": [{"producto": "...", "cantidad": n}]}. Temperatura 0 para que la misma consulta dé
 * la misma interpretación. Si Ollama no responde a tiempo o devuelve algo ilegible, lanza
 * ServicioNoDisponibleException (503) y lo deja en el log.
 */
@Component
public class OllamaChatbotAdapter implements ChatbotProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatbotAdapter.class);

    static final String URL_POR_DEFECTO = "http://localhost:11434";
    static final String MODELO_POR_DEFECTO = "qwen2.5:3b";
    static final String MENSAJE_NO_DISPONIBLE =
            "El asistente no está disponible en este momento. Probá armar el presupuesto con el formulario.";

    /** JSON Schema de la respuesta: Ollama obliga al modelo a respetarlo. */
    private static final Map<String, Object> FORMATO_RESPUESTA = Map.of(
            "type", "object",
            "properties", Map.of("items", Map.of(
                    "type", "array",
                    "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "producto", Map.of("type", "string"),
                                    "cantidad", Map.of("type", "integer")),
                            "required", List.of("producto", "cantidad")))),
            "required", List.of("items"));

    private final RestClient restClient;
    private final String modelo;
    private final ObjectMapper objectMapper;

    @Autowired
    public OllamaChatbotAdapter(RestClient.Builder builder, ObjectMapper objectMapper,
            @Value("${app.chatbot.url:}") String url,
            @Value("${app.chatbot.modelo:}") String modelo,
            @Value("${app.chatbot.timeout-ms:60000}") long timeoutMs) {
        this(construirCliente(builder, url, timeoutMs), modelo, objectMapper);
    }

    /** Para los tests: recibe el RestClient ya armado (por ejemplo, con MockRestServiceServer). */
    OllamaChatbotAdapter(RestClient restClient, String modelo, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.modelo = vacio(modelo) ? MODELO_POR_DEFECTO : modelo.trim();
        this.objectMapper = objectMapper;
    }

    private static RestClient construirCliente(RestClient.Builder builder, String url, long timeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        // Sin GPU el modelo puede tardar varios segundos en contestar
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        return builder.baseUrl(vacio(url) ? URL_POR_DEFECTO : url.trim()).requestFactory(fabrica).build();
    }

    @Override
    public List<ItemInterpretado> interpretar(String mensaje, List<String> productosDelCatalogo) {
        SolicitudChat solicitud = new SolicitudChat(modelo, false, FORMATO_RESPUESTA, Map.of("temperature", 0),
                List.of(new MensajeChat("system", instrucciones(productosDelCatalogo)),
                        new MensajeChat("user", mensaje)));
        RespuestaChat respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(solicitud)
                    .retrieve()
                    .body(RespuestaChat.class);
        } catch (Exception ex) {
            log.warn("No se pudo consultar a Ollama: {}", ex.getMessage());
            throw new ServicioNoDisponibleException(MENSAJE_NO_DISPONIBLE);
        }
        return leerItems(respuesta);
    }

    /** Traduce el texto JSON que escribió el modelo a la lista del dominio, descartando renglones inválidos. */
    private List<ItemInterpretado> leerItems(RespuestaChat respuesta) {
        if (respuesta == null || respuesta.message() == null || vacio(respuesta.message().content())) {
            log.warn("Ollama respondió sin contenido.");
            throw new ServicioNoDisponibleException(MENSAJE_NO_DISPONIBLE);
        }
        try {
            ContenidoModelo contenido = objectMapper.readValue(respuesta.message().content(), ContenidoModelo.class);
            if (contenido.items() == null) {
                return List.of();
            }
            return contenido.items().stream()
                    .filter(item -> item != null && !vacio(item.producto()) && item.cantidad() != null
                            && item.cantidad() > 0)
                    .map(item -> new ItemInterpretado(item.producto().trim(), item.cantidad()))
                    .toList();
        } catch (Exception ex) {
            log.warn("Ollama devolvió un JSON que no se pudo leer: {}", ex.getMessage());
            throw new ServicioNoDisponibleException(MENSAJE_NO_DISPONIBLE);
        }
    }

    /** Prompt de sistema: solo extraer productos y cantidades, usando los nombres del catálogo. */
    static String instrucciones(List<String> productosDelCatalogo) {
        String lista = productosDelCatalogo == null || productosDelCatalogo.isEmpty()
                ? "(el catálogo está vacío)"
                : "- " + String.join("\n- ", productosDelCatalogo);
        return """
                Sos el asistente de presupuestos de Dulce Mayor, un mayorista de golosinas.
                Tu única tarea es extraer del mensaje del cliente qué productos pide y en qué cantidad.
                Reglas:
                1. Si lo que pide se corresponde con un producto de la lista, usá exactamente el nombre de la lista.
                2. Si pide algo que no está en la lista, devolvelo con el nombre que usó el cliente.
                3. La cantidad es un número entero mayor que cero; si no la dice, usá 1.
                4. No calcules precios, totales ni stock, y no agregues productos que el cliente no pidió.
                5. Si el mensaje no pide ningún producto, devolvé la lista vacía.
                Respondé solo con JSON: {"items": [{"producto": "nombre", "cantidad": 1}]}
                Productos del catálogo:
                %s""".formatted(lista);
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    // ---- Forma del JSON de la API de Ollama (solo los campos que se usan) ----

    private record SolicitudChat(String model, boolean stream, Object format, Map<String, Object> options,
            List<MensajeChat> messages) {
    }

    private record MensajeChat(String role, String content) {
    }

    private record RespuestaChat(MensajeChat message) {
    }

    private record ContenidoModelo(List<ItemModelo> items) {
    }

    private record ItemModelo(String producto, Integer cantidad) {
    }
}
