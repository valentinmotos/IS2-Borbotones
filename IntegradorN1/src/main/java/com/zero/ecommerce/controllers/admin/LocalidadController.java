package com.zero.ecommerce.controllers.admin;

import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.services.DepartamentoService;
import com.zero.ecommerce.services.LocalidadService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/configuracion/ubicaciones")
public class LocalidadController {

    private final LocalidadService localidadService;
    private final DepartamentoService departamentoService;

    public LocalidadController(LocalidadService localidadService, DepartamentoService departamentoService) {
        this.localidadService = localidadService;
        this.departamentoService = departamentoService;
    }

    @GetMapping
    public String listarLocalidades(Model model) {
        model.addAttribute("localidades", localidadService.listarLocalidades());
        return "admin/ubicacion/ubicacion";
    }

    @GetMapping("/nueva")
    public String nuevaLocalidad(Model model) {
        model.addAttribute(
                "departamentos",
                departamentoService.listarDepartamentos()
        );
        return "admin/ubicacion/form";
    }

    // =========================================================
    // GUARDAR LOCALIDAD
    // =========================================================

    @PostMapping("/guardar")
    public String guardarLocalidad(
            @RequestParam("nombre") String nombre,
            @RequestParam("codigoPostal") String codigoPostal,
            @RequestParam("departamentoId") String departamentoId,
            RedirectAttributes redirectAttributes) {

        try {
            localidadService.crearLocalidad(
                    nombre,
                    codigoPostal,
                    departamentoId
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Localidad creada correctamente"
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/configuracion/ubicaciones";
    }

    // =========================================================
    // FORMULARIO MODIFICAR LOCALIDAD
    // =========================================================

    @GetMapping("/{id}/editar")
    public String editarLocalidad(
            @PathVariable String id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Localidad localidad = localidadService.buscarLocalidad(id);

            model.addAttribute(
                    "localidad",
                    localidad
            );

            model.addAttribute(
                    "departamentos",
                    departamentoService.listarDepartamentos()
            );

            return "admin/ubicacion/form";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/admin/configuracion/ubicaciones";
        }
    }

    // =========================================================
    // MODIFICAR LOCALIDAD
    // =========================================================

    @PostMapping("/{id}/editar")
    public String modificarLocalidad(
            @PathVariable String id,
            @RequestParam("nombre") String nombre,
            @RequestParam("codigoPostal") String codigoPostal,
            @RequestParam("departamentoId") String departamentoId,
            RedirectAttributes redirectAttributes) {

        try {
            localidadService.modificarLocalidad(
                    id,
                    nombre,
                    codigoPostal,
                    departamentoId
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Localidad modificada correctamente"
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/configuracion/ubicaciones";
    }

    // =========================================================
    // ELIMINAR LOCALIDAD
    // =========================================================

    @PostMapping("/{id}/eliminar")
    public String eliminarLocalidad(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {
            localidadService.eliminarLocalidad(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Localidad eliminada correctamente"
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/admin/configuracion/ubicaciones";
    }
}