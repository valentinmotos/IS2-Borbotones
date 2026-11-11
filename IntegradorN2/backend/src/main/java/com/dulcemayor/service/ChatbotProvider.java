package com.dulcemayor.service;

import java.util.List;

/**
 * Patrón Adapter (E3-04) · participante: Target. Interfaz propia del dominio para interpretar el
 * pedido escrito por un visitante sin depender de un motor de IA concreto. El adaptador
 * (OllamaChatbotAdapter) traduce la API de Ollama (Adaptee) a una lista de ItemInterpretado.
 *
 * La IA solo interpreta texto: devuelve qué productos y cuántos pidió el visitante. Nunca calcula
 * precios ni decide el stock; eso lo hace PresupuestoService contra el catálogo.
 */
public interface ChatbotProvider {

    /**
     * Interpreta el mensaje del visitante.
     *
     * @param mensaje             texto libre, por ejemplo «quiero 10 alfajores y 5 chupetines»
     * @param productosDelCatalogo nombres de los productos del catálogo, para que la IA use esos
     *                            mismos nombres cuando el pedido se corresponde con uno
     * @return productos y cantidades pedidas (puede estar vacía si el mensaje no pide nada)
     * @throws com.dulcemayor.exception.ServicioNoDisponibleException si el motor de IA no responde
     *                                                                o devuelve algo ilegible
     */
    List<ItemInterpretado> interpretar(String mensaje, List<String> productosDelCatalogo);

    /** Un renglón del pedido tal como lo entendió la IA (todavía sin validar contra el catálogo). */
    record ItemInterpretado(String producto, int cantidad) {
    }
}
