package com.zero.ecommerce.controllers.admin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.zero.ecommerce.dto.FilaTablaImagenDTO;
import com.zero.ecommerce.dto.ProductoForm;
import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.StockService;

@Controller
@RequestMapping("/admin/productos")
public class ProductoController {

    private static final String BASE = "/admin/productos";
    private static final int TAMANIO_PAGINA = 10;
    private final ProductoService service;
    private final CategoriaService categoriaService;
    private final StockService stockService;

    public ProductoController(ProductoService service, CategoriaService categoriaService, StockService stockService) {
        this.service = service;
        this.categoriaService = categoriaService;
        this.stockService = stockService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "productos";
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "") String categoria, @RequestParam(defaultValue = "") String subcategoria,
            @RequestParam(defaultValue = "") String oferta, @RequestParam(defaultValue = "1") int page,
            Model model) {
        Page<FilaTablaImagenDTO> productos = service.listarFilaProductoActivo(buscar, categoria, subcategoria,
                convertirOferta(oferta), page, TAMANIO_PAGINA);
        model.addAttribute("pageTitle", "Productos");
        model.addAttribute("encabezados", List.of("Código", "Nombre", "Talle", "Subcategoría", "Oferta",
                "Precio vigente", "Stock actual"));
        model.addAttribute("productos", productos.getContent());
        model.addAttribute("totalProductos", productos.getTotalElements());
        model.addAttribute("paginaActual", productos.getNumber() + 1);
        model.addAttribute("totalPaginas", productos.getTotalPages());
        model.addAttribute("paginacionUrl", urlListado(buscar, categoria, subcategoria, oferta));
        model.addAttribute("arbolCategorias", categoriaService.listarArbolActivo());
        model.addAttribute("opcionesOferta", opcionesOferta());
        model.addAttribute("buscar", buscar);
        model.addAttribute("categoria", categoria);
        model.addAttribute("subcategoria", subcategoria);
        model.addAttribute("oferta", oferta);
        return "admin/productos/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return formulario(model, null, new ProductoForm(), null);
    }

    /** Detalle del producto con su stock actual y el historial de movimientos (E3-04). */
    @GetMapping("/{id}")
    public String detalle(@PathVariable String id, Model model) {
        Producto producto = buscarO404(id);
        model.addAttribute("pageTitle", producto.getNombre() + " (talle " + producto.getTalle() + ")");
        model.addAttribute("producto", producto);
        model.addAttribute("stockActual", stockService.buscarStockActual(id));
        model.addAttribute("movimientos", stockService.listarHistorial(id));
        return "admin/productos/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Producto producto = buscarO404(id);
        return formulario(model, id, ProductoForm.desde(producto),
                producto.getImagen() == null ? null : producto.getImagen().getId());
    }

    @PostMapping
    public String crear(@ModelAttribute ProductoForm form,
            @RequestParam(required = false) MultipartFile imagen, RedirectAttributes flash) {
        try {
            service.crearProductoConImagen(form.getCodigo(), form.getNombre(), form.getDescripcion(),
                    form.getTalle(), form.isEnOferta(), imagen, form.getSubCategoriaId());
            flash.addFlashAttribute("exito", "Producto creado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, form, e);
            return "redirect:" + BASE + "/nuevo";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @ModelAttribute ProductoForm form,
            @RequestParam(required = false) MultipartFile imagen, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarProductoConImagen(id, form.getNombre(), form.getDescripcion(), form.getTalle(),
                    form.isEnOferta(), imagen, form.getSubCategoriaId());
            flash.addFlashAttribute("exito", "Producto modificado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, form, e);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.eliminarProducto(id);
            flash.addFlashAttribute("exito", "Producto eliminado correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    private String formulario(Model model, String id, ProductoForm form, String imagenId) {
        model.addAttribute("pageTitle", id == null ? "Nuevo producto" : "Editar producto");
        model.addAttribute("id", id);
        model.addAttribute("imagenId", imagenId);
        model.addAttribute("arbolCategorias", categoriaService.listarArbolActivo());
        if (!model.containsAttribute("productoForm")) {
            model.addAttribute("productoForm", form);
        }
        // El código no se edita: en la edición se muestra siempre el guardado.
        if (id != null) {
            ((ProductoForm) model.getAttribute("productoForm")).setCodigo(form.getCodigo());
        }
        return "admin/productos/formulario";
    }

    private void errorFormulario(RedirectAttributes flash, ProductoForm form, ErrorServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        flash.addFlashAttribute("productoForm", form);
    }

    private Boolean convertirOferta(String oferta) {
        return switch (oferta) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> null;
        };
    }

    private Map<String, String> opcionesOferta() {
        Map<String, String> opciones = new LinkedHashMap<>();
        opciones.put("true", "En oferta");
        opciones.put("false", "Sin oferta");
        return opciones;
    }

    /** URL del listado con los filtros aplicados, para que la paginación los conserve. */
    private String urlListado(String buscar, String categoria, String subcategoria, String oferta) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath(BASE);
        agregarFiltro(url, "buscar", buscar);
        agregarFiltro(url, "categoria", categoria);
        agregarFiltro(url, "subcategoria", subcategoria);
        agregarFiltro(url, "oferta", oferta);
        return url.encode().build().toUriString();
    }

    private void agregarFiltro(UriComponentsBuilder url, String nombre, String valor) {
        if (!valor.isBlank()) {
            url.queryParam(nombre, valor.strip());
        }
    }

    private Producto buscarO404(String id) {
        try {
            return service.buscarProducto(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
