package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.entities.Producto;
import com.zero.ecommerce.entities.VigenciaPrecio;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ProductoService;
import com.zero.ecommerce.services.VigenciaPrecioService;

@Controller
@RequestMapping("/admin")
public class PreciosController {

    private final ProductoService productoService;
    private final VigenciaPrecioService vigenciaPrecioService;

    public PreciosController(ProductoService productoService, VigenciaPrecioService vigenciaPrecioService) {
        this.productoService = productoService;
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "precios";
    }

    @GetMapping("/precios")
    public String listado(Model model,
            @RequestParam(value = "productoId", required = false) String productoId) {
        model.addAttribute("pageTitle", "Precios");
        List<Producto> productos = productoService.listarProductoActivo();
        model.addAttribute("productos", productos);
        if (productoId != null && !productoId.isBlank()) {
            Producto producto = buscarProducto(productoId);
            model.addAttribute("productoSeleccionado", producto);
            try {
                List<VigenciaPrecio> historial = vigenciaPrecioService.listarVigencias(producto.getId());
                model.addAttribute("historial", historial);
                model.addAttribute("vigenciaActual", vigenciaPrecioService.buscarVigenciaVigente(producto.getId()));
            } catch (ErrorServiceException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "admin/precios/listado";
    }

    @PostMapping("/precios")
    public String crearVigencia(@RequestParam String productoId,
            @RequestParam double precio,
            @RequestParam(required = false) LocalDate fechaDesde,
            RedirectAttributes flash) {
        try {
            LocalDate fecha = fechaDesde == null ? LocalDate.now() : fechaDesde;
            vigenciaPrecioService.crearVigenciaPrecio(productoId, fecha, precio);
            flash.addFlashAttribute("exito", "Se registró la nueva vigencia de precio.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/precios?productoId=" + productoId;
    }

    private Producto buscarProducto(String id) {
        try {
            return productoService.buscarProducto(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
