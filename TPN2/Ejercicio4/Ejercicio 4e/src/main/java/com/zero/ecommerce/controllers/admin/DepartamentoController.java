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

import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.DepartamentoService;
import com.zero.ecommerce.services.ProvinciaService;

@Controller
@RequestMapping("/admin/configuracion/ubicacion/departamentos")
public class DepartamentoController {

    private static final String BASE = "/admin/configuracion/ubicacion/departamentos";
    private final DepartamentoService service;
    private final ProvinciaService provinciaService;

    public DepartamentoController(DepartamentoService service, ProvinciaService provinciaService) {
        this.service = service;
        this.provinciaService = provinciaService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "ubicacion";
    }

    @ModelAttribute("pestanaActiva")
    public String pestanaActiva() {
        return "departamentos";
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String provincia, Model model) {
        model.addAttribute("pageTitle", "Departamentos");
        model.addAttribute("encabezados", List.of("Nombre", "Provincia"));
        model.addAttribute("registros", service.listarFilaDepartamentoActivo(provincia));
        model.addAttribute("baseUrl", BASE);
        model.addAttribute("urlNuevo",
                BASE + "/nuevo" + (provincia == null || provincia.isBlank() ? "" : "?provincia=" + provincia));
        model.addAttribute("textoNuevo", "Nuevo departamento");
        model.addAttribute("entidad", "departamento");
        model.addAttribute("filtroNombre", "provincia");
        model.addAttribute("filtroEtiqueta", "Provincia");
        model.addAttribute("filtroOpciones", provinciaService.listarOpcionProvinciaActivo());
        model.addAttribute("filtroSeleccionado", provincia);
        return "admin/ubicacion/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) String provincia, Model model) {
        return formulario(model, null, "", provincia);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Departamento departamento = buscarO404(id);
        return formulario(model, id, departamento.getNombre(), departamento.getProvincia().getId());
    }

    @PostMapping
    public String crear(@RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String provinciaId, RedirectAttributes flash) {
        try {
            service.crearDepartamento(nombre, provinciaId);
            flash.addFlashAttribute("exito", "Departamento creado correctamente.");
            return "redirect:" + BASE + "?provincia=" + provinciaId;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, provinciaId, e);
            return "redirect:" + BASE + "/nuevo";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String provinciaId, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarDepartamento(id, nombre, provinciaId);
            flash.addFlashAttribute("exito", "Departamento modificado correctamente.");
            return "redirect:" + BASE + "?provincia=" + provinciaId;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, provinciaId, e);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        Departamento departamento = buscarO404(id);
        try {
            service.eliminarDepartamento(id);
            flash.addFlashAttribute("exito", "Departamento eliminado correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE + "?provincia=" + departamento.getProvincia().getId();
    }

    private String formulario(Model model, String id, String nombre, String provinciaId) {
        model.addAttribute("pageTitle", id == null ? "Nuevo departamento" : "Editar departamento");
        model.addAttribute("seccion", "Departamentos");
        model.addAttribute("action", id == null ? BASE : BASE + "/" + id + "/editar");
        model.addAttribute("cancelarUrl", BASE);
        model.addAttribute("id", id);
        model.addAttribute("padreCampo", "provinciaId");
        model.addAttribute("padreEtiqueta", "Provincia");
        model.addAttribute("padreOpciones", provinciaService.listarOpcionProvinciaActivo());
        if (!model.containsAttribute("nombre")) {
            model.addAttribute("nombre", nombre);
        }
        if (!model.containsAttribute("padreSeleccionado")) {
            model.addAttribute("padreSeleccionado", provinciaId);
        }
        return "admin/ubicacion/formulario";
    }

    private void errorFormulario(RedirectAttributes flash, String nombre, String provinciaId,
            ErrorServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        flash.addFlashAttribute("nombre", nombre);
        flash.addFlashAttribute("padreSeleccionado", provinciaId);
    }

    private Departamento buscarO404(String id) {
        try {
            return service.buscarDepartamento(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
