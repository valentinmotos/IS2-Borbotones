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
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.DepartamentoService;
import com.zero.ecommerce.services.LocalidadService;

@Controller
@RequestMapping("/admin/configuracion/ubicacion/localidades")
public class LocalidadController {

    private static final String BASE = "/admin/configuracion/ubicacion/localidades";
    private final LocalidadService service;
    private final DepartamentoService departamentoService;

    public LocalidadController(LocalidadService service, DepartamentoService departamentoService) {
        this.service = service;
        this.departamentoService = departamentoService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "ubicacion";
    }

    @ModelAttribute("pestanaActiva")
    public String pestanaActiva() {
        return "localidades";
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String departamento, Model model) {
        model.addAttribute("pageTitle", "Localidades");
        model.addAttribute("encabezados", List.of("Nombre", "Código postal", "Departamento", "Provincia"));
        model.addAttribute("registros", service.listarFilaLocalidadActivo(departamento));
        model.addAttribute("baseUrl", BASE);
        model.addAttribute("urlNuevo", BASE + "/nueva"
                + (departamento == null || departamento.isBlank() ? "" : "?departamento=" + departamento));
        model.addAttribute("textoNuevo", "Nueva localidad");
        model.addAttribute("entidad", "localidad");
        model.addAttribute("filtroNombre", "departamento");
        model.addAttribute("filtroEtiqueta", "Departamento");
        model.addAttribute("filtroOpciones", departamentoService.listarOpcionDepartamentoActivo());
        model.addAttribute("filtroSeleccionado", departamento);
        return "admin/ubicacion/listado";
    }

    @GetMapping("/nueva")
    public String nueva(@RequestParam(required = false) String departamento, Model model) {
        return formulario(model, null, "", "", departamento);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Localidad localidad = buscarO404(id);
        return formulario(model, id, localidad.getNombre(), localidad.getCodigoPostal(),
                localidad.getDepartamento().getId());
    }

    @PostMapping
    public String crear(@RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String codigoPostal,
            @RequestParam(defaultValue = "") String departamentoId, RedirectAttributes flash) {
        try {
            service.crearLocalidad(nombre, codigoPostal, departamentoId);
            flash.addFlashAttribute("exito", "Localidad creada correctamente.");
            return "redirect:" + BASE + "?departamento=" + departamentoId;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, codigoPostal, departamentoId, e);
            return "redirect:" + BASE + "/nueva";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String codigoPostal,
            @RequestParam(defaultValue = "") String departamentoId, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarLocalidad(id, nombre, codigoPostal, departamentoId);
            flash.addFlashAttribute("exito", "Localidad modificada correctamente.");
            return "redirect:" + BASE + "?departamento=" + departamentoId;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, nombre, codigoPostal, departamentoId, e);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        Localidad localidad = buscarO404(id);
        try {
            service.eliminarLocalidad(id);
            flash.addFlashAttribute("exito", "Localidad eliminada correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE + "?departamento=" + localidad.getDepartamento().getId();
    }

    private String formulario(Model model, String id, String nombre, String codigoPostal, String departamentoId) {
        model.addAttribute("pageTitle", id == null ? "Nueva localidad" : "Editar localidad");
        model.addAttribute("seccion", "Localidades");
        model.addAttribute("action", id == null ? BASE : BASE + "/" + id + "/editar");
        model.addAttribute("cancelarUrl", BASE);
        model.addAttribute("id", id);
        model.addAttribute("conCodigoPostal", true);
        if (!model.containsAttribute("nombre")) {
            model.addAttribute("nombre", nombre);
        }
        if (!model.containsAttribute("codigoPostal")) {
            model.addAttribute("codigoPostal", codigoPostal);
        }
        if (!model.containsAttribute("padreSeleccionado")) {
            model.addAttribute("padreSeleccionado", departamentoId);
        }
        precargarCascada(model, (String) model.getAttribute("padreSeleccionado"));
        return "admin/ubicacion/formulario";
    }

    /** País → provincia → departamento en cascada: se precargan a partir del departamento elegido. */
    private void precargarCascada(Model model, String departamentoId) {
        model.addAttribute("conCascada", true);
        try {
            Departamento departamento = departamentoService.buscarDepartamento(departamentoId);
            model.addAttribute("cascadaDepartamentoId", departamento.getId());
            model.addAttribute("cascadaProvinciaId", departamento.getProvincia().getId());
            model.addAttribute("cascadaPaisId", departamento.getProvincia().getPais().getId());
        } catch (ErrorServiceException e) {
            // Sin departamento válido (alta o id inexistente): los selects arrancan vacíos.
        }
    }

    private void errorFormulario(RedirectAttributes flash, String nombre, String codigoPostal,
            String departamentoId, ErrorServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        flash.addFlashAttribute("nombre", nombre);
        flash.addFlashAttribute("codigoPostal", codigoPostal);
        flash.addFlashAttribute("padreSeleccionado", departamentoId);
    }

    private Localidad buscarO404(String id) {
        try {
            return service.buscarLocalidad(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
