package com.zero.ecommerce.controllers.publico;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.UsuarioService;

/** Registro de clientes (RF01) y activación de la cuenta con el código enviado por correo (RF02). */
@Controller
@RequestMapping("/registro")
public class RegistroController {

    private static final String ACTIVAR = "/registro/activar";
    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String registro(Model model) {
        model.addAttribute("pageTitle", "Crear cuenta");
        return "publico/registro";
    }

    @PostMapping
    public String registrar(@RequestParam(defaultValue = "") String correo,
            @RequestParam(defaultValue = "") String clave, @RequestParam(defaultValue = "") String confirmacion,
            RedirectAttributes flash) {
        try {
            Usuario usuario = usuarioService.registrarCliente(correo, clave, confirmacion);
            usuarioService.enviarCodigoActivacion(usuario, urlActivacion(usuario.getNombreUsuario()));
            flash.addFlashAttribute("exito", "Te enviamos un código de activación a " + usuario.getNombreUsuario()
                    + ". Ingresalo para activar tu cuenta.");
            return "redirect:" + rutaActivar(usuario.getNombreUsuario());
        } catch (ErrorServiceException e) {
            // Las claves no vuelven al formulario.
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("correo", correo);
            return "redirect:/registro";
        }
    }

    /** El link del correo llega con ?correo=, así solo falta escribir el código. */
    @GetMapping("/activar")
    public String activar(@RequestParam(defaultValue = "") String correo, Model model) {
        model.addAttribute("pageTitle", "Activar cuenta");
        if (!model.containsAttribute("correo")) {
            model.addAttribute("correo", correo);
        }
        return "publico/activar";
    }

    @PostMapping("/activar")
    public String confirmarActivacion(@RequestParam(defaultValue = "") String correo,
            @RequestParam(defaultValue = "") String codigo, RedirectAttributes flash) {
        try {
            usuarioService.activarCuenta(correo, codigo);
            flash.addFlashAttribute("exito", "¡Tu cuenta quedó activada! Ya podés ingresar.");
            return "redirect:/login";
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:" + rutaActivar(correo);
        }
    }

    @PostMapping("/reenviar")
    public String reenviarCodigo(@RequestParam(defaultValue = "") String correo, RedirectAttributes flash) {
        try {
            Usuario usuario = usuarioService.reenviarCodigoActivacion(correo);
            usuarioService.enviarCodigoActivacion(usuario, urlActivacion(usuario.getNombreUsuario()));
            flash.addFlashAttribute("exito", "Te enviamos un código nuevo a " + usuario.getNombreUsuario()
                    + ". El anterior ya no sirve.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + rutaActivar(correo);
    }

    private String rutaActivar(String correo) {
        if (correo == null || correo.isBlank()) {
            return ACTIVAR;
        }
        return UriComponentsBuilder.fromPath(ACTIVAR).queryParam("correo", "{correo}").encode()
                .buildAndExpand(correo.strip()).toUriString();
    }

    // URL absoluta para el correo: el envío corre en otro hilo, sin request (ver README, correos).
    // El correo va como variable para que se codifique entero (un "+" de un alias no se pierde).
    private String urlActivacion(String correo) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(ACTIVAR)
                .queryParam("correo", "{correo}").encode().buildAndExpand(correo).toUriString();
    }
}
