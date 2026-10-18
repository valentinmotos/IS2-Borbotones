package com.zero.ecommerce.controllers.admin;

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

import com.zero.ecommerce.entities.Nacionalidad;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.NacionalidadService;

@Controller
@RequestMapping("/admin/nacionalidades")
public class NacionalidadController {

    private static final String BASE = "/admin/nacionalidades";
    private final NacionalidadService service;

    public NacionalidadController(NacionalidadService service) {
        this.service = service;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "nacionalidades";
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pageTitle", "Nacionalidades");
        model.addAttribute("nacionalidades", service.listarNacionalidadActiva());
        return "admin/nacionalidades/listado";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        return formulario(model, null, "");
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Nacionalidad nacionalidad = buscarO404(id);
        return formulario(model, id, nacionalidad.getNombre());
    }

    @PostMapping
    public String crear(@RequestParam(defaultValue = "") String nombre, RedirectAttributes flash) {
        try {
            service.crearNacionalidad(nombre);
            flash.addFlashAttribute("exito", "Nacionalidad creada correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, e);
            return "redirect:" + BASE + "/nueva";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @RequestParam(defaultValue = "") String nombre,
            RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarNacionalidad(id, nombre);
            flash.addFlashAttribute("exito", "Nacionalidad modificada correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, e);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.eliminarNacionalidad(id);
            flash.addFlashAttribute("exito", "Nacionalidad eliminada correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    private String formulario(Model model, String id, String nombre) {
        model.addAttribute("pageTitle", id == null ? "Nueva nacionalidad" : "Editar nacionalidad");
        model.addAttribute("id", id);
        if (!model.containsAttribute("nombre")) {
            model.addAttribute("nombre", nombre);
        }
        return "admin/nacionalidades/formulario";
    }

    private void errorFormulario(RedirectAttributes flash, String nombre, ErrorServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        flash.addFlashAttribute("nombre", nombre);
    }

    private Nacionalidad buscarO404(String id) {
        try {
            return service.buscarNacionalidad(id);
        } catch (ErrorServiceException e) {
            // Sin causa de negocio: el advice global también inspecciona las causas y redirigiría.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
