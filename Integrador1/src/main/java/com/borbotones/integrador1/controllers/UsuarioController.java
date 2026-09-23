package com.borbotones.integrador1.controllers;

import com.borbotones.integrador1.dto.CambioClaveForm;
import com.borbotones.integrador1.dto.UsuarioForm;
import com.borbotones.integrador1.entities.Usuario;
import com.borbotones.integrador1.services.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(@RequestParam(name = "buscar", required = false) String buscar, Model model) {
        model.addAttribute("usuarios", usuarioService.buscarUsuariosActivos(buscar));
        model.addAttribute("buscar", buscar == null ? "" : buscar);
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuarioForm", usuarioService.crearFormularioUsuario());
        model.addAttribute("roles", usuarioService.listarRoles());
        return "usuarios/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("usuarioForm", usuarioService.crearFormularioEdicion(id));
            model.addAttribute("roles", usuarioService.listarRoles());
            return "usuarios/formulario";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/crear")
    public String crear(@ModelAttribute("usuarioForm") UsuarioForm form,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        form.setId(null);
        try {
            usuarioService.crearUsuario(form.getNombreUsuario(), form.getClave(), form.getRol());
            redirectAttributes.addFlashAttribute("exito", "El usuario fue creado correctamente");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("roles", usuarioService.listarRoles());
            return "usuarios/formulario";
        }
    }

    @PostMapping("/modificar")
    public String modificar(@ModelAttribute("usuarioForm") UsuarioForm form,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            usuarioService.modificarUsuario(form.getId(), form.getNombreUsuario(), form.getRol());
            redirectAttributes.addFlashAttribute("exito", "El usuario fue modificado correctamente");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("roles", usuarioService.listarRoles());
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
            model.addAttribute("cambioClaveForm", usuarioService.crearFormularioCambioClave());
            return "usuarios/clave";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/{id}/clave")
    public String modificarClave(@PathVariable String id,
                                 @ModelAttribute("cambioClaveForm") CambioClaveForm form,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
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

}
