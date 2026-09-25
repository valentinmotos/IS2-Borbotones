package com.zero.ecommerce.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.dto.FilaTablaDTO;
import com.zero.ecommerce.entities.Imagen;
import com.zero.ecommerce.entities.enums.TipoImagen;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ImagenService;

@Controller
public class DevController {

    private final ImagenService imagenService;

    public DevController(ImagenService imagenService) {
        this.imagenService = imagenService;
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
        return "dev/componentes";
    }

    @GetMapping("/dev/imagen")
    public String imagen(Model model, @RequestParam(value = "id", required = false) String id) {
        model.addAttribute("tiposImagen", TipoImagen.values());
        if (id != null && !id.isBlank()) {
            try {
                Imagen imagen = imagenService.buscarImagen(id);
                model.addAttribute("imagenActual", imagen);
                model.addAttribute("imagenActualUrl", "/imagen/" + imagen.getId());
            } catch (ErrorServiceException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "dev/imagen";
    }

    @PostMapping("/dev/imagen")
    public String guardarImagen(@RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "tipoImagen", required = false, defaultValue = "PRODUCTO") String tipoImagen,
            @RequestParam(value = "imagenId", required = false) String imagenId,
            RedirectAttributes redirectAttributes) {
        try {
            TipoImagen tipo = TipoImagen.valueOf(tipoImagen);
            Imagen imagen;
            if (imagenId != null && !imagenId.isBlank()) {
                imagen = imagenService.modificarImagen(imagenId, archivo, tipo);
                redirectAttributes.addFlashAttribute("exito", "La imagen se actualizó correctamente.");
            } else {
                imagen = imagenService.crearImagen(archivo, tipo);
                redirectAttributes.addFlashAttribute("exito", "La imagen se guardó correctamente.");
            }
            return "redirect:/dev/imagen?id=" + imagen.getId();
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dev/imagen";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "El tipo de imagen no es válido.");
            return "redirect:/dev/imagen";
        }
    }
}
