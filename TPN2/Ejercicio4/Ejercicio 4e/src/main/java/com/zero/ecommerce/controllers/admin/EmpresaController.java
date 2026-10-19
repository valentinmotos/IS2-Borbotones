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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.dto.DireccionForm;
import com.zero.ecommerce.dto.EmpresaForm;
import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.EmpresaService;

@Controller
@RequestMapping("/admin/configuracion/empresa")
public class EmpresaController {

    private static final String BASE = "/admin/configuracion/empresa";
    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "empresa";
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pageTitle", "Empresa");
        model.addAttribute("encabezados", List.of("Razón social", "CUIT", "Tipo", "Localidad"));
        model.addAttribute("empresas", service.listarFilaEmpresaActiva());
        return "admin/empresa/listado";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        return formulario(model, null, new EmpresaForm(), new DireccionForm());
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Empresa empresa = buscarO404(id);
        DireccionForm direccion = empresa.getDireccion() == null ? new DireccionForm()
                : DireccionForm.desde(empresa.getDireccion());
        return formulario(model, id, EmpresaForm.desde(empresa), direccion);
    }

    @PostMapping
    public String crear(@ModelAttribute EmpresaForm empresaForm, @ModelAttribute DireccionForm direccionForm,
            RedirectAttributes flash) {
        try {
            service.crearEmpresa(empresaForm.getRazonSocial(), empresaForm.getCuit(),
                    service.convertirTipoEmpresa(empresaForm.getTipoSucursal()), direccionForm,
                    empresaForm.getCorreo(), empresaForm.getTelefono(),
                    service.convertirTipoTelefono(empresaForm.getTipoTelefono()));
            flash.addFlashAttribute("exito", "Empresa creada correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, empresaForm, direccionForm, e);
            return "redirect:" + BASE + "/nueva";
        }
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @ModelAttribute EmpresaForm empresaForm,
            @ModelAttribute DireccionForm direccionForm, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.modificarEmpresa(id, empresaForm.getRazonSocial(), empresaForm.getCuit(),
                    service.convertirTipoEmpresa(empresaForm.getTipoSucursal()), direccionForm,
                    empresaForm.getCorreo(), empresaForm.getTelefono(),
                    service.convertirTipoTelefono(empresaForm.getTipoTelefono()));
            flash.addFlashAttribute("exito", "Empresa modificada correctamente.");
            return "redirect:" + BASE;
        } catch (ErrorServiceException e) {
            errorFormulario(flash, empresaForm, direccionForm, e);
            return "redirect:" + BASE + "/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        buscarO404(id);
        try {
            service.eliminarEmpresa(id);
            flash.addFlashAttribute("exito", "Empresa eliminada correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + BASE;
    }

    private String formulario(Model model, String id, EmpresaForm empresaForm, DireccionForm direccionForm) {
        model.addAttribute("pageTitle", id == null ? "Nueva empresa" : "Editar empresa");
        model.addAttribute("id", id);
        model.addAttribute("tiposEmpresa", service.listarTipoEmpresa());
        model.addAttribute("tiposTelefono", service.listarTipoTelefono());
        // Después de un error, los datos vuelven como flash y tienen prioridad sobre los guardados.
        if (!model.containsAttribute("empresaForm")) {
            model.addAttribute("empresaForm", empresaForm);
        }
        if (!model.containsAttribute("direccionForm")) {
            model.addAttribute("direccionForm", direccionForm);
        }
        return "admin/empresa/formulario";
    }

    private void errorFormulario(RedirectAttributes flash, EmpresaForm empresaForm, DireccionForm direccionForm,
            ErrorServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        flash.addFlashAttribute("empresaForm", empresaForm);
        flash.addFlashAttribute("direccionForm", direccionForm);
    }

    private Empresa buscarO404(String id) {
        try {
            return service.buscarEmpresa(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
