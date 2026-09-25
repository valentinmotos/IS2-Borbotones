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

import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.PaisService;
import com.zero.ecommerce.services.ProvinciaService;

@Controller
@RequestMapping("/admin/configuracion/ubicacion/provincias")
public class ProvinciaController {

    private static final String BASE = "/admin/configuracion/ubicacion/provincias";
    private final ProvinciaService service;
    private final PaisService paisService;

    public ProvinciaController(ProvinciaService service, PaisService paisService) {
        this.service = service;
        this.paisService = paisService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "ubicacion";
    }

    @ModelAttribute("pestanaActiva")
    public String pestanaActiva() {
        return "provincias";
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String pais, Model model) {
        model.addAttribute("pageTitle", "Provincias");
        model.addAttribute("encabezados", List.of("Nombre", "País"));
        model.addAttribute("registros", service.listarFilaProvinciaActivo(pais));
        model.addAttribute("baseUrl", BASE);
        model.addAttribute("urlNuevo", BASE + "/nueva" + (pais == null || pais.isBlank() ? "" : "?pais=" + pais));
        model.addAttribute("textoNuevo", "Nueva provincia");
        model.addAttribute("entidad", "provincia");
        model.addAttribute("filtroNombre", "pais");
        model.addAttribute("filtroEtiqueta", "País");
        model.addAttribute("filtroOpciones", paisService.listarOpcionPaisActivo());
        model.addAttribute("filtroSeleccionado", pais);
        return "admin/ubicacion/listado";
    }

    @GetMapping("/nueva")
    public String nueva(@RequestParam(required = false) String pais, Model model) {
        return formulario(model, null, "", pais);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Provincia provincia = buscarO404(id);
        return formulario(model, id, provincia.getNombre(), provincia.getPais().getId());
    }

    @PostMapping
    public String crear(@RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String paisId, RedirectAttributes flash) {
        try {
            service.crearProvincia(nombre, paisId);
            flash.addFlashAttribute("exito", "Provincia creada correctamente.");
            return "redirect:" + BASE + "?pais=" + paisId;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, paisId, e);
            return "redirect:" + BASE + "/nueva";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String paisId, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarProvincia(id, nombre, paisId);
            flash.addFlashAttribute("exito", "Provincia modificada correctamente.");
            return "redirect:" + BASE + "?pais=" + paisId;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, paisId, e);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        Provincia provincia = buscarO404(id);
        try {
            service.eliminarProvincia(id);
            flash.addFlashAttribute("exito", "Provincia eliminada correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE + "?pais=" + provincia.getPais().getId();
    }

    private String formulario(Model model, String id, String nombre, String paisId) {
        model.addAttribute("pageTitle", id == null ? "Nueva provincia" : "Editar provincia");
        model.addAttribute("seccion", "Provincias");
        model.addAttribute("action", id == null ? BASE : BASE + "/" + id + "/editar");
        model.addAttribute("cancelarUrl", BASE);
        model.addAttribute("id", id);
        model.addAttribute("padreCampo", "paisId");
        model.addAttribute("padreEtiqueta", "País");
        model.addAttribute("padreOpciones", paisService.listarOpcionPaisActivo());
        if (!model.containsAttribute("nombre")) {
            model.addAttribute("nombre", nombre);
        }
        if (!model.containsAttribute("padreSeleccionado")) {
            model.addAttribute("padreSeleccionado", paisId);
        }
        return "admin/ubicacion/formulario";
    }

    private void errorFormulario(RedirectAttributes flash, String nombre, String paisId, ErrorServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        flash.addFlashAttribute("nombre", nombre);
        flash.addFlashAttribute("padreSeleccionado", paisId);
    }

    private Provincia buscarO404(String id) {
        try {
            return service.buscarProvincia(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
