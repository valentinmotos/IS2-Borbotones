package com.zero.ecommerce.controllers.admin;

import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.services.UbicacionService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ubicaciones")
public class UbicacionController {

    private final UbicacionService ubicacionService;


    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    @GetMapping
    public String listarLocalidades(Model model) {

        model.addAttribute("localidades", ubicacionService.listarLocalidades() );

        return "admin/ubicacion/ubicacion";
    }

    @GetMapping("/nueva")
    public String nuevaLocalidad(Model model) {

        model.addAttribute(
                "departamentos",
                ubicacionService.listarDepartamentos()
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

            ubicacionService.crearLocalidad(
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

        return "redirect:/admin/ubicaciones";
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

            Localidad localidad = ubicacionService.buscarLocalidad(id);

            model.addAttribute(
                    "localidad",
                    localidad
            );

            model.addAttribute(
                    "departamentos",
                    ubicacionService.listarDepartamentos()
            );

            return "admin/ubicacion/form";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );

            return "redirect:/admin/ubicaciones";
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

            ubicacionService.modificarLocalidad(
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

        return "redirect:/admin/ubicaciones";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminarLocalidad(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {

            ubicacionService.eliminarLocalidad(id);

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

        return "redirect:/admin/ubicaciones";
    }
}
