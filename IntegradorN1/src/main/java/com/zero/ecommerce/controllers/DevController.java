package com.zero.ecommerce.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.dto.DireccionForm;
import com.zero.ecommerce.dto.FilaTablaDTO;
import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.DireccionService;

@Controller
public class DevController {

    private final DireccionService direccionService;

    public DevController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    @GetMapping("/dev/ejemplo-publico")
    public String ejemploPublico() {
        return "dev/ejemplo-publico";
    }

    @GetMapping("/dev/ejemplo-admin")
    public String ejemploAdmin() {
        return "dev/ejemplo-admin";
    }

    @GetMapping("/dev/componentes")
    public String componentes(Model model) {
        model.addAttribute("exito", "La operación se realizó correctamente.");
        model.addAttribute("error", "Este es un mensaje de validación de ejemplo.");
        model.addAttribute("encabezados", List.of("Producto", "Estado", "Precio"));
        model.addAttribute("filas", List.of(
            List.of("Remera Zero Pro", "Publicado", "$12.500"),
            List.of("Short Zero Move", "Pendiente", "$9.900")));
        model.addAttribute("filasVacias", List.of());
        model.addAttribute("encabezadosRegistros", List.of("Tipo de pago", "Observación"));
        model.addAttribute("registros", List.of(
            new FilaTablaDTO("demo-1", "Mercado Pago", List.of("Billetera virtual", "Mercado Pago")),
            new FilaTablaDTO("demo-2", "Banco Nación", List.of("Transferencia", "Banco Nación"))));
        Map<String, String> opciones = new LinkedHashMap<>();
        opciones.put("EFECTIVO", "Efectivo");
        opciones.put("TRANSFERENCIA", "Transferencia");
        opciones.put("BILLETERA_VIRTUAL", "Billetera virtual");
        model.addAttribute("opciones", opciones);
        Map<String, Map<String, String>> grupos = new LinkedHashMap<>();
        grupos.put("Argentina / Mendoza", Map.of("capital", "Capital", "maipu", "Maipú"));
        grupos.put("Argentina / San Juan", Map.of("rivadavia", "Rivadavia"));
        model.addAttribute("grupos", grupos);
        return "dev/componentes";
    }

    /** E1-04: prueba del fragment de dirección. Con ?id= precarga una dirección guardada. */
    @GetMapping("/dev/direccion")
    public String direccion(@RequestParam(required = false) String id, Model model) {
        model.addAttribute("pageTitle", "Prueba de dirección");
        model.addAttribute("id", id);
        if (!model.containsAttribute("direccionForm")) {
            DireccionForm form = new DireccionForm();
            if (id != null && !id.isBlank()) {
                try {
                    Direccion direccion = direccionService.buscarDireccion(id);
                    form = DireccionForm.desde(direccion);
                    model.addAttribute("direccion", direccion);
                } catch (ErrorServiceException e) {
                    model.addAttribute("error", e.getMessage());
                    model.addAttribute("id", null);
                }
            }
            model.addAttribute("direccionForm", form);
        }
        return "dev/direccion";
    }

    @PostMapping("/dev/direccion")
    public String guardarDireccion(@RequestParam(required = false) String id,
            @ModelAttribute DireccionForm form, RedirectAttributes flash) {
        boolean alta = id == null || id.isBlank();
        try {
            if (alta) {
                id = direccionService.crearDireccion(form.getCalle(), form.getNumeracion(), form.getBarrio(),
                        form.getManzanaPiso(), form.getCasaDepartamento(), form.getReferencia(),
                        form.getLocalidadId()).getId();
            } else {
                direccionService.modificarDireccion(id, form.getCalle(), form.getNumeracion(), form.getBarrio(),
                        form.getManzanaPiso(), form.getCasaDepartamento(), form.getReferencia(),
                        form.getLocalidadId());
            }
            flash.addFlashAttribute("exito", alta ? "Dirección creada correctamente."
                    : "Dirección modificada correctamente.");
            return "redirect:/dev/direccion?id=" + id;
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("direccionForm", form);
            return alta ? "redirect:/dev/direccion" : "redirect:/dev/direccion?id=" + id;
        }
    }
}
