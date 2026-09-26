package com.zero.ecommerce.controllers.admin;

import java.util.LinkedHashMap;
import java.util.Map;

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

import com.zero.ecommerce.dto.EmpleadoUsuarioForm;
import com.zero.ecommerce.entities.Empleado;
import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.EmpleadoService;
import com.zero.ecommerce.services.UsuarioService;

/** ABM exclusivo del JEFE. SecurityConfig protege todo /admin/usuarios/**. */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {
    private static final String BASE = "/admin/usuarios";
    private final UsuarioService usuarioService;
    private final EmpleadoService empleadoService;

    public UsuarioAdminController(UsuarioService usuarioService, EmpleadoService empleadoService) {
        this.usuarioService = usuarioService;
        this.empleadoService = empleadoService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() { return "usuarios"; }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String rol, @RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "") String buscar, Model model) {
        model.addAttribute("pageTitle", "Usuarios");
        model.addAttribute("usuarios", usuarioService.listarUsuarios(rol, estado, buscar));
        model.addAttribute("roles", opcionesRoles());
        model.addAttribute("estados", opcionesEstados());
        model.addAttribute("rol", rol);
        model.addAttribute("estado", estado);
        model.addAttribute("buscar", buscar);
        return "admin/usuarios/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) { return formulario(model, null, new EmpleadoUsuarioForm()); }

    @GetMapping("/{id}")
    public String ver(@PathVariable String id, Model model) {
        Usuario usuario = buscarO404(id);
        model.addAttribute("pageTitle", "Detalle de usuario");
        model.addAttribute("usuario", usuario);
        model.addAttribute("nombre", usuarioService.nombreParaMostrar(usuario));
        return "admin/usuarios/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Empleado empleado = buscarEmpleadoO404(id);
        return formulario(model, id, EmpleadoUsuarioForm.desde(empleado));
    }

    @PostMapping
    public String crear(@ModelAttribute EmpleadoUsuarioForm form, RedirectAttributes flash) {
        try {
            empleadoService.crearEmpleado(form.getNombre(), form.getApellido(),
                    empleadoService.convertirFechaNacimiento(form.getFechaNacimiento()),
                    empleadoService.convertirTipoDocumento(form.getTipoDocumento()), form.getNumeroDocumento(),
                    form.getCorreo(), usuarioService.convertirRolEmpleado(form.getRol()), form.getClave(),
                    form.getConfirmacionClave());
            flash.addFlashAttribute("exito", "Empleado creado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("empleadoForm", form);
            return "redirect:" + BASE + "/nuevo";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @ModelAttribute EmpleadoUsuarioForm form, RedirectAttributes flash) {
        buscarEmpleadoO404(id);
        try {
            empleadoService.modificarEmpleado(id, form.getNombre(), form.getApellido(),
                    empleadoService.convertirFechaNacimiento(form.getFechaNacimiento()),
                    empleadoService.convertirTipoDocumento(form.getTipoDocumento()), form.getNumeroDocumento(),
                    form.getCorreo(), usuarioService.convertirRolEmpleado(form.getRol()), form.getClave(),
                    form.getConfirmacionClave());
            flash.addFlashAttribute("exito", "Empleado modificado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("empleadoForm", form);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        Usuario usuario = buscarO404(id);
        try {
            String actual = usuarioService.usuarioActual().orElseThrow().getId();
            if (usuario.getRol().name().equals("CLIENTE")) usuarioService.eliminarUsuario(id, actual);
            else empleadoService.eliminarEmpleadoPorUsuario(id, actual);
            flash.addFlashAttribute("exito", "Usuario dado de baja correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable String id, RedirectAttributes flash) {
        buscarO404(id);
        try {
            usuarioService.reactivarCliente(id);
            flash.addFlashAttribute("exito", "Cliente reactivado correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    private String formulario(Model model, String id, EmpleadoUsuarioForm form) {
        model.addAttribute("pageTitle", id == null ? "Nuevo empleado" : "Editar empleado");
        model.addAttribute("id", id);
        model.addAttribute("formAction", id == null ? BASE : BASE + "/" + id + "/editar");
        model.addAttribute("tiposDocumento", empleadoService.listarTiposDocumento());
        model.addAttribute("rolesEmpleado", usuarioService.listarRolesEmpleado());
        if (!model.containsAttribute("empleadoForm")) model.addAttribute("empleadoForm", form);
        return "admin/usuarios/formulario";
    }

    private Usuario buscarO404(String id) {
        try { return usuarioService.buscarUsuarioAdministracion(id); }
        catch (ErrorServiceException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()); }
    }

    private Empleado buscarEmpleadoO404(String idUsuario) {
        try { return empleadoService.buscarEmpleadoPorUsuario(idUsuario); }
        catch (ErrorServiceException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()); }
    }

    private Map<String, String> opcionesRoles() {
        Map<String, String> roles = new LinkedHashMap<>();
        roles.put("", "Todos los roles");
        roles.put("JEFE", "Jefe");
        roles.put("ADMINISTRATIVO", "Administrativo");
        roles.put("CLIENTE", "Cliente");
        return roles;
    }

    private Map<String, String> opcionesEstados() {
        Map<String, String> estados = new LinkedHashMap<>();
        estados.put("", "Todos los estados");
        estados.put("ACTIVO", "Activo");
        estados.put("PENDIENTE", "Pendiente de activación");
        estados.put("INACTIVO", "Inactivo");
        return estados;
    }
}
