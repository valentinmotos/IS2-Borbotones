package com.zero.ecommerce.controllers.admin;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.entities.ConfiguracionCorreoEmpresa;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ConfiguracionCorreoEmpresaService;
import com.zero.ecommerce.services.EmailService;
import com.zero.ecommerce.services.EmpresaService;

/**
 * Configuración SMTP de la sede central: una sola pantalla que crea la configuración la primera vez
 * y después la edita. La clave nunca se envía a la vista.
 */
@Controller
@RequestMapping("/admin/configuracion/correo")
public class ConfiguracionCorreoController {

    private static final String BASE = "/admin/configuracion/correo";
    private final ConfiguracionCorreoEmpresaService service;
    private final EmpresaService empresaService;
    private final EmailService emailService;

    public ConfiguracionCorreoController(ConfiguracionCorreoEmpresaService service, EmpresaService empresaService,
            EmailService emailService) {
        this.service = service;
        this.empresaService = empresaService;
        this.emailService = emailService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "correo";
    }

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("pageTitle", "Correo");
        Optional<ConfiguracionCorreoEmpresa> configuracion;
        try {
            configuracion = service.encontrarConfiguracionCorreoEmpresa();
        } catch (ErrorServiceException e) {
            model.addAttribute("sinSede", e.getMessage());
            return "admin/correo/formulario";
        }
        model.addAttribute("configurado", configuracion.isPresent());
        // Valores por defecto de Gmail para la primera carga; los datos del flash tienen prioridad.
        agregarSiFalta(model, "correo", configuracion.map(ConfiguracionCorreoEmpresa::getCorreo).orElse(""));
        agregarSiFalta(model, "smtp", configuracion.map(ConfiguracionCorreoEmpresa::getSmtp).orElse("smtp.gmail.com"));
        agregarSiFalta(model, "puerto", configuracion.map(ConfiguracionCorreoEmpresa::getPuerto).orElse("587"));
        agregarSiFalta(model, "tls", configuracion.map(ConfiguracionCorreoEmpresa::isTls).orElse(true));
        agregarSiFalta(model, "destinatario", configuracion.map(ConfiguracionCorreoEmpresa::getCorreo).orElse(""));
        return "admin/correo/formulario";
    }

    @PostMapping
    public String guardar(@RequestParam(defaultValue = "") String correo, @RequestParam(defaultValue = "") String clave,
            @RequestParam(defaultValue = "") String puerto, @RequestParam(defaultValue = "") String smtp,
            @RequestParam(defaultValue = "false") boolean tls, RedirectAttributes flash) {
        try {
            String idEmpresa = empresaService.buscarSedeCentral().getId();
            Optional<ConfiguracionCorreoEmpresa> actual = service.encontrarConfiguracionCorreoEmpresa();
            if (actual.isPresent()) {
                service.modificarConfiguracionCorreoEmpresa(actual.get().getId(), correo, clave, puerto, smtp, tls,
                        idEmpresa);
            } else {
                service.crearConfiguracionCorreoEmpresa(correo, clave, puerto, smtp, tls, idEmpresa);
            }
            flash.addFlashAttribute("exito", "Configuración de correo guardada correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("correo", correo);
            flash.addFlashAttribute("puerto", puerto);
            flash.addFlashAttribute("smtp", smtp);
            flash.addFlashAttribute("tls", tls);
        }
        return "redirect:" + BASE;
    }

    @PostMapping("/prueba")
    public String enviarPrueba(@RequestParam(defaultValue = "") String destinatario, RedirectAttributes flash) {
        try {
            emailService.enviarPrueba(destinatario);
            flash.addFlashAttribute("exito", "Correo de prueba enviado a " + destinatario.strip()
                    + ". Revisá la bandeja de entrada y la carpeta de spam.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        flash.addFlashAttribute("destinatario", destinatario);
        return "redirect:" + BASE;
    }

    private void agregarSiFalta(Model model, String nombre, Object valor) {
        if (!model.containsAttribute(nombre)) {
            model.addAttribute(nombre, valor);
        }
    }
}
