package com.borbotones.integrador1.controllers;

import com.borbotones.integrador1.dto.CambioClaveForm;
import com.borbotones.integrador1.dto.UsuarioForm;
import com.borbotones.integrador1.entities.RolUsuario;
import com.borbotones.integrador1.entities.Usuario;
import com.borbotones.integrador1.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarUsuarioActivo());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        prepararFormulario(model, new UsuarioForm());
        return "usuarios/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioService.buscarUsuario(id);
            UsuarioForm form = new UsuarioForm();
            form.setId(usuario.getId());
            form.setNombreUsuario(usuario.getNombreUsuario());
            form.setRol(usuario.getRol());
            prepararFormulario(model, form);
            return "usuarios/formulario";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("usuarioForm") UsuarioForm form,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            prepararFormulario(model, form);
            return "usuarios/formulario";
        }
        try {
            if (form.getId() == null || form.getId().isBlank()) {
                usuarioService.crearUsuario(form.getNombreUsuario(), form.getClave(), form.getRol());
            } else {
                usuarioService.modificarUsuario(form.getId(), form.getNombreUsuario(), form.getClave(), form.getRol());
            }
            redirectAttributes.addFlashAttribute("exito", "La accion fue realizada correctamente");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            prepararFormulario(model, form);
            return "usuarios/formulario";
        }
    }

    @PostMapping("/baja/{id}")
    public String baja(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminarUsuario(id);
            redirectAttributes.addFlashAttribute("exito", "El usuario fue dado de baja");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/{id}/clave")
    public String cambioClave(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioService.buscarUsuario(id);
            model.addAttribute("usuario", usuario);
            model.addAttribute("cambioClaveForm", new CambioClaveForm());
            return "usuarios/clave";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/{id}/clave")
    public String modificarClave(@PathVariable String id,
                                 @Valid @ModelAttribute("cambioClaveForm") CambioClaveForm form,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("usuario", usuarioService.buscarUsuario(id));
            return "usuarios/clave";
        }
        try {
            usuarioService.modificarClave(id, form.getClaveActual(), form.getNuevaClave(), form.getConfirmarClave());
            redirectAttributes.addFlashAttribute("exito", "La clave fue modificada correctamente");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("usuario", usuarioService.buscarUsuario(id));
            model.addAttribute("error", ex.getMessage());
            return "usuarios/clave";
        }
    }

    private void prepararFormulario(Model model, UsuarioForm form) {
        model.addAttribute("usuarioForm", form);
        model.addAttribute("roles", RolUsuario.values());
    }
}
