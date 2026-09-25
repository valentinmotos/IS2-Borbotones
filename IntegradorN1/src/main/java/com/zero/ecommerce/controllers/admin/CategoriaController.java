package com.zero.ecommerce.controllers.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zero.ecommerce.dto.CategoriaArbolDTO;
import com.zero.ecommerce.dto.CategoriaForm;
import com.zero.ecommerce.dto.SubCategoriaForm;
import com.zero.ecommerce.entities.Categoria;
import com.zero.ecommerce.entities.SubCategoria;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.CategoriaService;
import com.zero.ecommerce.services.SubCategoriaService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final SubCategoriaService subCategoriaService;

    public CategoriaController(CategoriaService categoriaService, SubCategoriaService subCategoriaService) {
        this.categoriaService = categoriaService;
        this.subCategoriaService = subCategoriaService;
    }

    @ModelAttribute("menuActivo")
    public String menuActivo() {
        return "categorias";
    }

    @GetMapping
    public String listar(Model model) {
        List<CategoriaArbolDTO> categorias = categoriaService.listarArbolActivo();
        model.addAttribute("pageTitle", "Categorías");
        model.addAttribute("categorias", categorias);
        return "admin/categorias/listado";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("pageTitle", "Nueva categoría");
        if (!model.containsAttribute("categoriaForm")) {
            model.addAttribute("categoriaForm", new CategoriaForm());
        }
        return "admin/categorias/formulario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("categoriaForm") CategoriaForm form,
            BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", "El nombre de la categoría es obligatorio.");
            flash.addFlashAttribute("org.springframework.validation.BindingResult.categoriaForm", result);
            flash.addFlashAttribute("categoriaForm", form);
            return "redirect:/admin/categorias/nueva";
        }
        try {
            categoriaService.crearCategoria(form.getNombre());
            flash.addFlashAttribute("exito", "La categoría se guardó correctamente.");
            return "redirect:/admin/categorias";
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("categoriaForm", form);
            return "redirect:/admin/categorias/nueva";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Categoria categoria = buscarCategoria(id);
        CategoriaForm form = new CategoriaForm();
        form.setNombre(categoria.getNombre());
        model.addAttribute("pageTitle", "Editar categoría");
        model.addAttribute("id", id);
        model.addAttribute("categoriaForm", form);
        return "admin/categorias/formulario";
    }

    @PostMapping("/{id}/editar")
    public String modificar(@PathVariable String id, @Valid @ModelAttribute("categoriaForm") CategoriaForm form,
            BindingResult result, RedirectAttributes flash) {
        buscarCategoria(id);
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", "El nombre de la categoría es obligatorio.");
            flash.addFlashAttribute("org.springframework.validation.BindingResult.categoriaForm", result);
            flash.addFlashAttribute("categoriaForm", form);
            return "redirect:/admin/categorias/" + id + "/editar";
        }
        try {
            categoriaService.modificarCategoria(id, form.getNombre());
            flash.addFlashAttribute("exito", "La categoría se actualizó correctamente.");
            return "redirect:/admin/categorias";
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("categoriaForm", form);
            return "redirect:/admin/categorias/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes flash) {
        buscarCategoria(id);
        try {
            categoriaService.eliminarCategoria(id);
            flash.addFlashAttribute("exito", "La categoría se eliminó correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    @GetMapping("/{categoriaId}/subcategorias/nueva")
    public String nuevaSubCategoria(@PathVariable String categoriaId, Model model) {
        buscarCategoria(categoriaId);
        model.addAttribute("pageTitle", "Nueva subcategoría");
        model.addAttribute("categoriaId", categoriaId);
        if (!model.containsAttribute("subCategoriaForm")) {
            SubCategoriaForm form = new SubCategoriaForm();
            form.setCategoriaId(categoriaId);
            model.addAttribute("subCategoriaForm", form);
        }
        model.addAttribute("categorias", categoriaService.listarCategoriaActiva());
        return "admin/categorias/subcategoria-formulario";
    }

    @PostMapping("/{categoriaId}/subcategorias")
    public String crearSubCategoria(@PathVariable String categoriaId,
            @Valid @ModelAttribute("subCategoriaForm") SubCategoriaForm form,
            BindingResult result, RedirectAttributes flash) {
        buscarCategoria(categoriaId);
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", "El nombre de la subcategoría es obligatorio.");
            flash.addFlashAttribute("org.springframework.validation.BindingResult.subCategoriaForm", result);
            flash.addFlashAttribute("subCategoriaForm", form);
            return "redirect:/admin/categorias/" + categoriaId + "/subcategorias/nueva";
        }
        try {
            subCategoriaService.crearSubCategoria(categoriaId, form.getNombre());
            flash.addFlashAttribute("exito", "La subcategoría se guardó correctamente.");
            return "redirect:/admin/categorias";
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("subCategoriaForm", form);
            return "redirect:/admin/categorias/" + categoriaId + "/subcategorias/nueva";
        }
    }

    @GetMapping("/{categoriaId}/subcategorias/{id}/editar")
    public String editarSubCategoria(@PathVariable String categoriaId, @PathVariable String id, Model model) {
        buscarCategoria(categoriaId);
        SubCategoria subCategoria = buscarSubCategoria(id);
        SubCategoriaForm form = new SubCategoriaForm();
        form.setNombre(subCategoria.getNombre());
        form.setCategoriaId(subCategoria.getCategoria().getId());
        model.addAttribute("pageTitle", "Editar subcategoría");
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("id", id);
        model.addAttribute("subCategoriaForm", form);
        model.addAttribute("categorias", categoriaService.listarCategoriaActiva());
        return "admin/categorias/subcategoria-formulario";
    }

    @PostMapping("/{categoriaId}/subcategorias/{id}/editar")
    public String modificarSubCategoria(@PathVariable String categoriaId, @PathVariable String id,
            @Valid @ModelAttribute("subCategoriaForm") SubCategoriaForm form,
            BindingResult result, RedirectAttributes flash) {
        buscarCategoria(categoriaId);
        buscarSubCategoria(id);
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", "El nombre de la subcategoría es obligatorio.");
            flash.addFlashAttribute("org.springframework.validation.BindingResult.subCategoriaForm", result);
            flash.addFlashAttribute("subCategoriaForm", form);
            return "redirect:/admin/categorias/" + categoriaId + "/subcategorias/" + id + "/editar";
        }
        try {
            String categoriaDestino = form.getCategoriaId() == null || form.getCategoriaId().isBlank() ? categoriaId : form.getCategoriaId();
            subCategoriaService.modificarSubCategoria(id, categoriaDestino, form.getNombre());
            flash.addFlashAttribute("exito", "La subcategoría se actualizó correctamente.");
            return "redirect:/admin/categorias";
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("subCategoriaForm", form);
            return "redirect:/admin/categorias/" + categoriaId + "/subcategorias/" + id + "/editar";
        }
    }

    @PostMapping("/{categoriaId}/subcategorias/{id}/eliminar")
    public String eliminarSubCategoria(@PathVariable String categoriaId, @PathVariable String id, RedirectAttributes flash) {
        buscarCategoria(categoriaId);
        buscarSubCategoria(id);
        try {
            subCategoriaService.eliminarSubCategoria(id);
            flash.addFlashAttribute("exito", "La subcategoría se eliminó correctamente.");
        } catch (ErrorServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    private Categoria buscarCategoria(String id) {
        try {
            return categoriaService.buscarCategoria(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private SubCategoria buscarSubCategoria(String id) {
        try {
            return subCategoriaService.buscarSubCategoria(id);
        } catch (ErrorServiceException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
