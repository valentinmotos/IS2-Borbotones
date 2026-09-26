package com.zero.ecommerce.controllers.admin;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

import com.zero.ecommerce.dto.NewsletterEnvioResultadoDTO;
import com.zero.ecommerce.dto.NewsletterEstadoDTO;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.NewsletterService;

@Controller
@RequestMapping("/admin/newsletter")
public class NewsletterController {

    private static final String VISTA = "admin/newsletter/index";
    private static final String REDIRECT = "redirect:/admin/newsletter";
    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "newsletter";
    }

    @GetMapping
    public String ver(Model model) {
        NewsletterEstadoDTO estado = newsletterService.obtenerEstado();
        model.addAttribute("pageTitle", "Newsletter de ofertas");
        model.addAttribute("estado", estado);
        return VISTA;
    }

    @GetMapping(value = "/vista-previa", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> vistaPrevia() {
        try {
            return ResponseEntity.ok(newsletterService.renderizarVistaPrevia());
        } catch (ErrorServiceException e) {
            String mensaje = HtmlUtils.htmlEscape(e.getMessage());
            return ResponseEntity.internalServerError().body("<!doctype html><html lang=\"es\"><body>"
                    + "<p>No se pudo generar la vista previa: " + mensaje + "</p></body></html>");
        }
    }

    @PostMapping("/enviar")
    public String enviar(RedirectAttributes flash) {
        try {
            NewsletterEnvioResultadoDTO resultado = newsletterService.enviar();
            flash.addFlashAttribute("exito", "Newsletter enviado correctamente a "
                    + resultado.cantidadEnviada() + " cliente(s). El próximo envío será el "
                    + resultado.proximoEnvio() + ".");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return REDIRECT;
    }
}
