package com.zero.ecommerce.controllers.admin;

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

import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.PaisService;

@Controller
@RequestMapping("/admin/configuracion/ubicacion")
public class PaisController {

    private static final String BASE = "/admin/configuracion/ubicacion/paises";
    private final PaisService service;

    public PaisController(PaisService service) {
        this.service = service;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "ubicacion";
    }

    @ModelAttribute("pestanaActiva")
    public String pestanaActiva() {
        return "paises";
    }

    /** La entrada del menú abre la primera pestaña. */
    @GetMapping
    public String inicio() {
        return "redirect:" + BASE;
    }

    @GetMapping("/paises")
    public String listar(Model model) {
        model.addAttribute("pageTitle", "Países");
        model.addAttribute("encabezados", List.of("Nombre"));
        model.addAttribute("registros", service.listarFilaPaisActivo());
        model.addAttribute("baseUrl", BASE);
        model.addAttribute("urlNuevo", BASE + "/nuevo");
        model.addAttribute("textoNuevo", "Nuevo país");
        model.addAttribute("entidad", "país");
        return "admin/ubicacion/listado";
    }

    @GetMapping("/paises/nuevo")
    public String nuevo(Model model) {
        return formulario(model, null, "");
    }

    @GetMapping("/paises/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Pais pais = buscarO404(id);
        return formulario(model, id, pais.getNombre());
    }

    @PostMapping("/paises")
    public String crear(@RequestParam(defaultValue = "") String nombre, RedirectAttributes flash) {
        try {
            service.crearPais(nombre);
            flash.addFlashAttribute("exito", "País creado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, e);
            return "redirect:" + BASE + "/nuevo";
        }
    }

    @PostMapping("/paises/{id}/editar")
    public String modificar(@PathVariable String id, @RequestParam(defaultValue = "") String nombre,
            RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarPais(id, nombre);
            flash.addFlashAttribute("exito", "País modificado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, e);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/paises/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.eliminarPais(id);
            flash.addFlashAttribute("exito", "País eliminado correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    private String formulario(Model model, String id, String nombre) {
        model.addAttribute("pageTitle", id == null ? "Nuevo país" : "Editar país");
        model.addAttribute("seccion", "Países");
        model.addAttribute("action", id == null ? BASE : BASE + "/" + id + "/editar");
        model.addAttribute("cancelarUrl", BASE);
        model.addAttribute("id", id);
        if (!model.containsAttribute("nombre")) {
            model.addAttribute("nombre", nombre);
        }
        return "admin/ubicacion/formulario";
    }

    private void errorFormulario(RedirectAttributes flash, String nombre, ErrorServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        flash.addFlashAttribute("nombre", nombre);
    }

    private Pais buscarO404(String id) {
        try {
            return service.buscarPais(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
