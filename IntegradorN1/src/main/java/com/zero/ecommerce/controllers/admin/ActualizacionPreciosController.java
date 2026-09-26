package com.zero.ecommerce.controllers.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.dto.ActualizacionPrecioDTO;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.VigenciaPrecioService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/precios/actualizacion")
public class ActualizacionPreciosController {

    private static final String ACTUALIZACION_PENDIENTE = "actualizacionPreciosPendiente";

    private final CategoriaService categoriaService;
    private final VigenciaPrecioService vigenciaPrecioService;

    public ActualizacionPreciosController(CategoriaService categoriaService,
            VigenciaPrecioService vigenciaPrecioService) {
        this.categoriaService = categoriaService;
        this.vigenciaPrecioService = vigenciaPrecioService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "precios-actualizacion";
    }

    @GetMapping
    public String formulario(Model model) {
        prepararFormulario(model, "TODO", null, null, null, LocalDate.now(), List.of());
        return "admin/precios/actualizacion";
    }

    @PostMapping("/vista-previa")
    public String vistaPrevia(@RequestParam(defaultValue = "TODO") String alcance,
            @RequestParam(required = false) String categoriaId,
            @RequestParam(required = false) String subCategoriaId,
            @RequestParam(required = false) Double porcentaje,
            @RequestParam(required = false) LocalDate fechaDesde, Model model, HttpSession session) {
        List<ActualizacionPrecioDTO> preview = List.of();
        try {
            String idAlcance = idAlcance(alcance, categoriaId, subCategoriaId);
            double porcentajeValidado = porcentaje == null ? Double.NaN : porcentaje;
            preview = vigenciaPrecioService.previsualizarActualizacionMasiva(alcance,
                    idAlcance, porcentajeValidado, fechaDesde);
            session.setAttribute(ACTUALIZACION_PENDIENTE,
                    new ActualizacionPendiente(alcance, idAlcance, porcentajeValidado, fechaDesde, preview));
        } catch (ErrorServiceException e) {
            session.removeAttribute(ACTUALIZACION_PENDIENTE);
            model.addAttribute("error", e.getMessage());
        }
        prepararFormulario(model, alcance, categoriaId, subCategoriaId, porcentaje, fechaDesde, preview);
        return "admin/precios/actualizacion";
    }

    @PostMapping("/confirmar")
    public String confirmar(HttpSession session, RedirectAttributes flash) {
        ActualizacionPendiente pendiente = (ActualizacionPendiente) session.getAttribute(ACTUALIZACION_PENDIENTE);
        if (pendiente == null) {
            flash.addFlashAttribute("error", "Primero generá una vista previa de la actualización.");
            return "redirect:/admin/precios/actualizacion";
        }

        try {
            int cantidad = vigenciaPrecioService.confirmarActualizacionMasiva(pendiente.alcance(),
                    pendiente.idAlcance(), pendiente.porcentaje(), pendiente.fechaDesde(), pendiente.vistaPrevia());
            flash.addFlashAttribute("exito", "Precios actualizados para " + cantidad
                    + (cantidad == 1 ? " producto." : " productos."));
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } finally {
            session.removeAttribute(ACTUALIZACION_PENDIENTE);
        }
        return "redirect:/admin/precios/actualizacion";
    }

    private void prepararFormulario(Model model, String alcance, String categoriaId, String subCategoriaId,
            Double porcentaje, LocalDate fechaDesde, List<ActualizacionPrecioDTO> preview) {
        model.addAttribute("pageTitle", "Actualización por inflación");
        model.addAttribute("categorias", categoriaService.listarArbolActivo());
        model.addAttribute("alcance", alcance);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("subCategoriaId", subCategoriaId);
        model.addAttribute("porcentaje", porcentaje);
        model.addAttribute("fechaDesde", fechaDesde == null ? LocalDate.now() : fechaDesde);
        model.addAttribute("preview", preview);
    }

    private String idAlcance(String alcance, String categoriaId, String subCategoriaId) {
        return switch (alcance) {
            case "CATEGORIA" -> categoriaId;
            case "SUBCATEGORIA" -> subCategoriaId;
            default -> null;
        };
    }

    private record ActualizacionPendiente(String alcance, String idAlcance, double porcentaje,
            LocalDate fechaDesde, List<ActualizacionPrecioDTO> vistaPrevia) {
    }
}
