package com.tp2.cliente.controller;

import com.tp2.cliente.dto.ProveedorDto;
import com.tp2.cliente.service.ProveedorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/proveedores")
public class ProveedorWebController {
    private final ProveedorService proveedorService;

    public ProveedorWebController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public String listar(Model model) {
        return mostrar(model, new ProveedorDto());
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model) {
        return mostrar(model, proveedorService.buscar(id));
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute ProveedorDto proveedor, RedirectAttributes attributes) {
        proveedorService.guardar(proveedor);
        attributes.addFlashAttribute("mensaje", "Proveedor guardado.");
        return "redirect:/proveedores";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes attributes) {
        proveedorService.eliminar(id);
        attributes.addFlashAttribute("mensaje", "Proveedor eliminado.");
        return "redirect:/proveedores";
    }

    @ExceptionHandler(RestClientException.class)
    public String manejarErrorBackend(RestClientException exception, HttpServletRequest request, Model model, RedirectAttributes attributes) {
        String error = "No se pudo completar la operacion: " + exception.getMessage();
        if ("GET".equals(request.getMethod())) {
            model.addAttribute("error", error);
            model.addAttribute("proveedores", List.of());
            model.addAttribute("proveedor", new ProveedorDto());
            return "proveedores";
        }
        attributes.addFlashAttribute("error", error);
        return "redirect:/proveedores";
    }

    private String mostrar(Model model, ProveedorDto proveedor) {
        model.addAttribute("proveedores", proveedorService.listar());
        model.addAttribute("proveedor", proveedor);
        return "proveedores";
    }
}
