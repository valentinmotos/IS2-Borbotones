package com.tp2.cliente.controller;

import com.tp2.cliente.service.ArticuloService;
import com.tp2.cliente.service.CarritoService;
import com.tp2.cliente.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
public class CarritoWebController {
    private final UsuarioService usuarioService;
    private final ArticuloService articuloService;
    private final CarritoService carritoService;

    public CarritoWebController(UsuarioService usuarioService, ArticuloService articuloService, CarritoService carritoService) {
        this.usuarioService = usuarioService;
        this.articuloService = articuloService;
        this.carritoService = carritoService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("articulos", articuloService.listar());
        model.addAttribute("carritos", carritoService.listar());
        return "index";
    }

    @PostMapping("/carritos")
    public String crear(@RequestParam String usuarioId, RedirectAttributes attributes) {
        carritoService.crear(usuarioId);
        attributes.addFlashAttribute("mensaje", "Carrito creado correctamente.");
        return "redirect:/";
    }

    @PostMapping("/carritos/{carritoId}/detalles")
    public String agregar(@PathVariable String carritoId, @RequestParam String articuloId, @RequestParam int cantidad, RedirectAttributes attributes) {
        carritoService.agregarArticulo(carritoId, articuloId, cantidad);
        attributes.addFlashAttribute("mensaje", "Articulo agregado al carrito.");
        return "redirect:/";
    }

    @PostMapping("/carritos/{carritoId}/detalles/{detalleId}")
    public String editar(@PathVariable String carritoId, @PathVariable String detalleId, @RequestParam int cantidad, RedirectAttributes attributes) {
        carritoService.editarDetalle(carritoId, detalleId, cantidad);
        attributes.addFlashAttribute("mensaje", "Detalle actualizado.");
        return "redirect:/";
    }

    @PostMapping("/carritos/{carritoId}/eliminar")
    public String eliminar(@PathVariable String carritoId, RedirectAttributes attributes) {
        carritoService.eliminar(carritoId);
        attributes.addFlashAttribute("mensaje", "Carrito eliminado.");
        return "redirect:/";
    }

    @PostMapping("/carritos/{carritoId}/finalizar")
    public String finalizar(@PathVariable String carritoId, RedirectAttributes attributes) {
        carritoService.finalizar(carritoId);
        attributes.addFlashAttribute("mensaje", "Compra finalizada.");
        return "redirect:/";
    }

    @ExceptionHandler(RestClientException.class)
    public String manejarErrorBackend(RestClientException exception, RedirectAttributes attributes) {
        attributes.addFlashAttribute("error", "No se pudo completar la operacion: " + exception.getMessage());
        return "redirect:/";
    }
}
