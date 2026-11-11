package com.dulcemayor.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.dulcemayor.dto.CotizacionDolarResponse;
import com.dulcemayor.dto.ItemPresupuestoDTO;
import com.dulcemayor.dto.PresupuestoResponse;
import com.dulcemayor.dto.ProductoExcluidoDTO;
import com.dulcemayor.entity.Producto;
import com.dulcemayor.entity.Stock;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.exception.RecursoNoEncontradoException;
import com.dulcemayor.repository.ProductoRepository;
import com.dulcemayor.repository.StockRepository;
import com.dulcemayor.service.ChatbotProvider.ItemInterpretado;
import com.dulcemayor.utils.TextoUtils;

/**
 * Presupuesto del chatbot (E3-04). La IA (ChatbotProvider) solo interpreta el texto y devuelve
 * productos y cantidades; este service valida cada renglón contra el catálogo, el stock (E2-02) y
 * el precio vigente (E2-03), y calcula subtotales, total en pesos y total en dólares (E1-08).
 *
 * Sin @Transactional a propósito: la consulta a la IA puede tardar varios segundos y no tiene que
 * retener una conexión de la base. Solo lee; no reserva stock.
 */
@Service
public class PresupuestoService {

    static final String MOTIVO_NO_ENCONTRADO = "No está en el catálogo.";
    static final String MOTIVO_AMBIGUO = "Coincide con varios productos: indicá cuál querés.";
    static final String MOTIVO_SIN_STOCK = "Sin stock.";
    static final String MOTIVO_SIN_PRECIO = "No tiene precio vigente.";

    private final ChatbotProvider chatbot;
    private final ProductoRepository productos;
    private final StockRepository stocks;
    private final VigenciaPrecioService precios;
    private final CotizacionDolarService cotizaciones;

    public PresupuestoService(ChatbotProvider chatbot, ProductoRepository productos, StockRepository stocks,
            VigenciaPrecioService precios, CotizacionDolarService cotizaciones) {
        this.chatbot = chatbot;
        this.productos = productos;
        this.stocks = stocks;
        this.precios = precios;
        this.cotizaciones = cotizaciones;
    }

    /**
     * Arma el presupuesto a partir del mensaje del visitante, por ejemplo «quiero 10 alfajores y 5 chupetines».
     *
     * @throws ErrorServiceException 400 si el mensaje está vacío
     * @throws com.dulcemayor.exception.ServicioNoDisponibleException 503 si la IA no responde
     */
    public PresupuestoResponse armarPresupuesto(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new ErrorServiceException("Escribí qué productos querés presupuestar.", "mensaje");
        }
        // Se le pasan todos los productos activos (también los sin stock) para poder decir por qué se excluye
        List<Producto> catalogo = productos.findByEliminadoFalse();
        List<String> nombres = catalogo.stream().map(Producto::getNombre).distinct().toList();

        List<ItemPresupuestoDTO> items = new ArrayList<>();
        List<ProductoExcluidoDTO> excluidos = new ArrayList<>();
        for (ItemInterpretado pedido : agrupar(chatbot.interpretar(mensaje.strip(), nombres))) {
            evaluar(pedido, catalogo, items, excluidos);
        }

        BigDecimal total = items.stream().map(ItemPresupuestoDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        Optional<CotizacionDolarResponse> cotizacion = cotizacionVigente();
        BigDecimal totalDolares = cotizacion.filter(c -> c.valorVenta() > 0)
                .map(c -> total.divide(BigDecimal.valueOf(c.valorVenta()), 2, RoundingMode.HALF_UP))
                .orElse(null);
        return new PresupuestoResponse(List.copyOf(items), List.copyOf(excluidos), total,
                cotizacion.map(CotizacionDolarResponse::valorVenta).orElse(null),
                cotizacion.map(CotizacionDolarResponse::fecha).orElse(null), totalDolares);
    }

    /** Valida un renglón y lo agrega a items o a excluidos. */
    private void evaluar(ItemInterpretado pedido, List<Producto> catalogo, List<ItemPresupuestoDTO> items,
            List<ProductoExcluidoDTO> excluidos) {
        List<Producto> coincidencias = buscarEnCatalogo(pedido.producto(), catalogo);
        if (coincidencias.size() != 1) {
            excluidos.add(new ProductoExcluidoDTO(pedido.producto(), pedido.cantidad(),
                    coincidencias.isEmpty() ? MOTIVO_NO_ENCONTRADO : MOTIVO_AMBIGUO));
            return;
        }
        Producto producto = coincidencias.get(0);
        int disponible = stocks.findByProducto_IdAndEliminadoFalse(producto.getId())
                .map(Stock::getCantidadActual).orElse(0);
        if (disponible <= 0) {
            excluidos.add(new ProductoExcluidoDTO(producto.getNombre(), pedido.cantidad(), MOTIVO_SIN_STOCK));
            return;
        }
        if (disponible < pedido.cantidad()) {
            excluidos.add(new ProductoExcluidoDTO(producto.getNombre(), pedido.cantidad(),
                    "Stock insuficiente (disponibles: " + disponible + ")."));
            return;
        }
        Optional<Double> precio = precios.buscarPrecioVigente(producto.getId());
        if (precio.isEmpty()) {
            excluidos.add(new ProductoExcluidoDTO(producto.getNombre(), pedido.cantidad(), MOTIVO_SIN_PRECIO));
            return;
        }
        BigDecimal unitario = BigDecimal.valueOf(precio.get()).setScale(2, RoundingMode.HALF_UP);
        items.add(new ItemPresupuestoDTO(producto.getId(), producto.getCodigo(), producto.getNombre(),
                producto.getPresentacion(), pedido.cantidad(), unitario,
                unitario.multiply(BigDecimal.valueOf(pedido.cantidad()))));
    }

    /** Palabras que no identifican al producto: conectores y formas de empaque («3 cajas de chicles»). */
    private static final Set<String> PALABRAS_IGNORADAS = Set.of("de", "del", "la", "las", "el", "los", "con",
            "y", "x", "caja", "cajas", "bolsa", "bolsas", "paquete", "paquetes", "pack", "packs", "bulto",
            "bultos", "unidad", "unidades");

    /**
     * Busca el producto que la IA nombró. La IA no siempre copia el nombre exacto («chicles de menta»
     * en vez de «Chicle de menta»), así que la coincidencia la decide el backend:
     * <ol>
     * <li>nombre exacto, sin importar mayúsculas ni tildes;</li>
     * <li>si no, productos cuyo nombre contiene todas las palabras pedidas, tolerando plurales
     * («alfajores» ↔ «Alfajor de chocolate»);</li>
     * <li>si quedan varios, se prefieren los que empiezan con la primera palabra pedida
     * («chocolates» → «Chocolate con leche» y no «Alfajor de chocolate»).</li>
     * </ol>
     * Devuelve todas las coincidencias: si no hay exactamente una, el renglón se excluye.
     */
    static List<Producto> buscarEnCatalogo(String nombre, List<Producto> catalogo) {
        String buscado = TextoUtils.normalizar(nombre);
        List<Producto> exactos = catalogo.stream()
                .filter(p -> TextoUtils.normalizar(p.getNombre()).equals(buscado)).toList();
        if (!exactos.isEmpty()) {
            return exactos;
        }
        List<String> pedidas = palabras(buscado);
        if (pedidas.isEmpty()) {
            return List.of();
        }
        List<Producto> candidatos = catalogo.stream()
                .filter(p -> {
                    List<String> delProducto = palabras(TextoUtils.normalizar(p.getNombre()));
                    return pedidas.stream().allMatch(
                            pedida -> delProducto.stream().anyMatch(propia -> mismaPalabra(pedida, propia)));
                })
                .toList();
        if (candidatos.size() <= 1) {
            return candidatos;
        }
        List<Producto> empiezanIgual = candidatos.stream()
                .filter(p -> {
                    List<String> delProducto = palabras(TextoUtils.normalizar(p.getNombre()));
                    return !delProducto.isEmpty() && mismaPalabra(pedidas.get(0), delProducto.get(0));
                })
                .toList();
        return empiezanIgual.isEmpty() ? candidatos : empiezanIgual;
    }

    /** Palabras significativas de un texto ya normalizado. */
    private static List<String> palabras(String texto) {
        return Arrays.stream(texto.split("[^\\p{Alnum}]+"))
                .filter(palabra -> !palabra.isEmpty() && !PALABRAS_IGNORADAS.contains(palabra))
                .toList();
    }

    /** Iguales salvo el plural: «chicles» = «chicle», «chupetines» = «chupetin», «alfajores» = «alfajor». */
    private static boolean mismaPalabra(String a, String b) {
        Set<String> formasDeA = formas(a);
        return formas(b).stream().anyMatch(formasDeA::contains);
    }

    private static Set<String> formas(String palabra) {
        Set<String> formas = new HashSet<>();
        formas.add(palabra);
        if (palabra.length() > 3 && palabra.endsWith("s")) {
            formas.add(palabra.substring(0, palabra.length() - 1));
        }
        if (palabra.length() > 4 && palabra.endsWith("es")) {
            formas.add(palabra.substring(0, palabra.length() - 2));
        }
        return formas;
    }

    /** Si la IA nombra dos veces el mismo producto, se suman las cantidades (respetando el orden). */
    private static List<ItemInterpretado> agrupar(List<ItemInterpretado> interpretados) {
        Map<String, ItemInterpretado> porNombre = new LinkedHashMap<>();
        for (ItemInterpretado item : interpretados) {
            porNombre.merge(TextoUtils.normalizar(item.producto()), item,
                    (a, b) -> new ItemInterpretado(a.producto(), a.cantidad() + b.cantidad()));
        }
        return List.copyOf(porNombre.values());
    }

    /** Cotización para el total en dólares; si no hay ninguna, el presupuesto sale solo en pesos. */
    private Optional<CotizacionDolarResponse> cotizacionVigente() {
        try {
            return Optional.ofNullable(cotizaciones.buscarCotizacionVigente());
        } catch (RecursoNoEncontradoException ex) {
            return Optional.empty();
        }
    }
}
