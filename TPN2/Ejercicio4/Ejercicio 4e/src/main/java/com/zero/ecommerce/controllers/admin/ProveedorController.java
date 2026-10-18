package com.zero.ecommerce.controllers.admin;

import java.util.LinkedHashMap;
import java.util.List;
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
        List<Proveedor> proveedores = service.listarProveedorActivo(buscar);
        model.addAttribute("pageTitle", "Proveedores");
        model.addAttribute("proveedores", proveedores);
        model.addAttribute("totalProveedores", proveedores.size());
        model.addAttribute("buscar", buscar);
        return "admin/proveedores/listado";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("pageTitle", "Nuevo proveedor");
        if (!model.containsAttribute("proveedorForm")) {
            model.addAttribute("proveedorForm", ProveedorForm.nuevo());
        }
        agregarOpciones(model);
        return "admin/proveedores/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Proveedor proveedor = buscarO404(id);
        model.addAttribute("pageTitle", "Editar proveedor");
        model.addAttribute("id", id);
        if (!model.containsAttribute("proveedorForm")) {
            model.addAttribute("proveedorForm", ProveedorForm.desde(proveedor));
        }
        agregarOpciones(model);
        return "admin/proveedores/formulario";
    }

    @PostMapping
    public String crear(@ModelAttribute ProveedorForm proveedorForm, RedirectAttributes flash) {
        try {
            service.crearProveedor(proveedorForm.getRazonSocial(), proveedorForm.getContactos());
            flash.addFlashAttribute("exito", "Proveedor creado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, proveedorForm, e);
            return "redirect:" + BASE + "/nuevo";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @ModelAttribute ProveedorForm proveedorForm,
            RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarProveedor(id, proveedorForm.getRazonSocial(), proveedorForm.getContactos());
            flash.addFlashAttribute("exito", "Proveedor modificado correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, proveedorForm, e);
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

    // Opciones de los selects de cada fila de contacto.
    private void agregarOpciones(Model model) {
        Map<String, String> tiposMedio = new LinkedHashMap<>();
        tiposMedio.put(ContactoItemDTO.CORREO, "Correo electrónico");
        tiposMedio.put(ContactoItemDTO.CELULAR, "Celular (WhatsApp)");
        tiposMedio.put(ContactoItemDTO.FIJO, "Teléfono fijo");
        Map<String, String> tiposContacto = new LinkedHashMap<>();
        tiposContacto.put("EMPRESA", "Empresa");
        tiposContacto.put("LABORAL", "Laboral");
        tiposContacto.put("PERSONAL", "Personal");
        model.addAttribute("tiposMedio", tiposMedio);
        model.addAttribute("tiposContacto", tiposContacto);
    }

    private void errorFormulario(RedirectAttributes flash, ProveedorForm proveedorForm, ErrorServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        flash.addFlashAttribute("proveedorForm", proveedorForm);
    }

    private Proveedor buscarO404(String id) {
        try {
            return service.buscarProveedor(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
