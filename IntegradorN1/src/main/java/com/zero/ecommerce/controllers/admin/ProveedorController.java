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

import com.zero.ecommerce.dto.ContactoItemDTO;
import com.zero.ecommerce.dto.ProveedorForm;
import com.zero.ecommerce.entities.Proveedor;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ProveedorService;

@Controller
@RequestMapping("/admin/proveedores")
public class ProveedorController {

    private static final String BASE = "/admin/proveedores";
    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "proveedores";
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String buscar, Model model) {
        model.addAttribute("pageTitle", "Proveedores");
        List<Proveedor> proveedores = service.buscar(buscar);
        model.addAttribute("proveedores", proveedores);
        model.addAttribute("totalProveedores", proveedores.size());
        model.addAttribute("buscar", buscar);
        return "admin/proveedores/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("pageTitle", "Nuevo proveedor");
        if (!model.containsAttribute("form")) {
            ProveedorForm form = new ProveedorForm();
            form.getContactos().add(new ContactoItemDTO(null, "CORREO", "", "EMPRESA", ""));
            form.getContactos().add(new ContactoItemDTO(null, "CELULAR", "", "LABORAL", ""));
            model.addAttribute("form", form);
        }
        return "admin/proveedores/formulario";
    }

    @GetMapping("/nueva")
    public String nueva() {
        return "redirect:" + BASE + "/nuevo";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Proveedor proveedor = buscarO404(id);
        model.addAttribute("pageTitle", "Editar proveedor");
        model.addAttribute("id", id);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", ProveedorForm.desde(proveedor));
        }
        return "admin/proveedores/formulario";
    }

    @PostMapping
    public String crear(@ModelAttribute ProveedorForm form, RedirectAttributes flash) {
        try {
            service.crearProveedor(form.getRazonSocial(), form.getContactos());
            flash.addFlashAttribute("exito", "Proveedor creado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("form", form);
            return "redirect:" + BASE + "/nuevo";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @ModelAttribute ProveedorForm form, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarProveedor(id, form.getRazonSocial(), form.getContactos());
            flash.addFlashAttribute("exito", "Proveedor modificado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("form", form);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.eliminarProveedor(id);
            flash.addFlashAttribute("exito", "Proveedor eliminado correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    private Proveedor buscarO404(String id) {
        try {
            return service.buscarProveedor(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
