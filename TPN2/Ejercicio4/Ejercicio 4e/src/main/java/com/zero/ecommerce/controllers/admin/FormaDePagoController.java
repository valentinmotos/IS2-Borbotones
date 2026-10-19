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

import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.FormaDePagoService;

@Controller
@RequestMapping("/admin/configuracion/formas-pago")
public class FormaDePagoController {

    private static final String BASE = "/admin/configuracion/formas-pago";
    private final FormaDePagoService service;

    public FormaDePagoController(FormaDePagoService service) {
        this.service = service;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "formas-pago";
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pageTitle", "Formas de pago");
        model.addAttribute("encabezados", List.of("Tipo de pago", "Observación"));
        model.addAttribute("formasDePago", service.listarFilaFormaDePagoActivo());
        return "admin/formas-pago/listado";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        return formulario(model, null, "", "");
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        FormaDePago formaDePago = buscarO404(id);
        return formulario(model, id, formaDePago.getTipoPago().name(), formaDePago.getObservacion());
    }

    @PostMapping
    public String crear(@RequestParam(defaultValue = "") String tipoPago,
            @RequestParam(defaultValue = "") String observacion, RedirectAttributes flash) {
        try {
            service.crearFormaDePago(service.convertirTipoPago(tipoPago), observacion);
            flash.addFlashAttribute("exito", "Forma de pago creada correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, tipoPago, observacion, e);
            return "redirect:" + BASE + "/nueva";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @RequestParam(defaultValue = "") String tipoPago,
            @RequestParam(defaultValue = "") String observacion, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarFormaDePago(id, service.convertirTipoPago(tipoPago), observacion);
            flash.addFlashAttribute("exito", "Forma de pago modificada correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, tipoPago, observacion, e);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.eliminarFormaDePago(id);
            flash.addFlashAttribute("exito", "Forma de pago eliminada correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    private String formulario(Model model, String id, String tipoPago, String observacion) {
        model.addAttribute("pageTitle", id == null ? "Nueva forma de pago" : "Editar forma de pago");
        model.addAttribute("id", id);
        model.addAttribute("tiposPago", service.listarTipoPago());
        if (!model.containsAttribute("tipoPago")) {
            model.addAttribute("tipoPago", tipoPago);
        }
        if (!model.containsAttribute("observacion")) {
            model.addAttribute("observacion", observacion);
        }
        return "admin/formas-pago/formulario";
    }

    private void errorFormulario(RedirectAttributes flash, String tipoPago, String observacion,
            ErrorServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        flash.addFlashAttribute("tipoPago", tipoPago);
        flash.addFlashAttribute("observacion", observacion);
    }

    private FormaDePago buscarO404(String id) {
        try {
            return service.buscarFormaDePago(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
