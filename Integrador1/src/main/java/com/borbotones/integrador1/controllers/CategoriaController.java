package com.borbotones.integrador1.controllers;

import com.borbotones.integrador1.entities.Categoria;
import com.borbotones.integrador1.services.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Controlador Spring MVC para la gestión de Categoria (ABM).
 * 
 * Se encarga de:
 * - Recibir peticiones HTTP.
 * - Recibir datos de formularios.
 * - Invocar al CategoriaService.
 * - Seleccionar las vistas Thymeleaf correspondientes.
 * - Enviar los datos necesarios al modelo.
 */
@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /**
     * Muestra el listado de categorías.
     * Permite filtrar entre solo activas o todas (incluidas bajas lógicas)
     * y buscar por coincidencia en el nombre.
     */
    @GetMapping
    public String listar(@RequestParam(value = "mostrarEliminadas", defaultValue = "false") boolean mostrarEliminadas,
                         @RequestParam(value = "criterio", required = false) String criterio,
                         Model model) {
        Collection<Categoria> categorias;
        if (criterio != null && !criterio.trim().isEmpty()) {
            categorias = categoriaService.buscarPorCriterio(criterio, mostrarEliminadas);
        } else if (mostrarEliminadas) {
            categorias = categoriaService.listarCategoria();
        } else {
            categorias = categoriaService.listarCategoriaActivo();
        }

        model.addAttribute("categorias", categorias);
        model.addAttribute("mostrarEliminadas", mostrarEliminadas);
        model.addAttribute("criterio", criterio != null ? criterio.trim() : "");
        model.addAttribute("titulo", "Zero | Gestión de Categorías");
        return "categorias/listar";
    }

    /**
     * Muestra el formulario para crear una nueva categoría.
     */
    @GetMapping("/crear")
    public String mostrarFormularioCrear(Model model) {
        if (!model.containsAttribute("nombre")) {
            model.addAttribute("nombre", "");
        }
        model.addAttribute("titulo", "Zero | Crear Categoría");
        return "categorias/crear";
    }

    /**
     * Procesa la creación de una nueva categoría.
     */
    @PostMapping("/crear")
    public String crear(@RequestParam("nombre") String nombre,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        try {
            categoriaService.crearCategoria(nombre);
            redirectAttributes.addFlashAttribute("exito", "Categoría creada con éxito.");
            return "redirect:/categorias";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("nombre", nombre);
            model.addAttribute("titulo", "Zero | Crear Categoría");
            return "categorias/crear";
        }
    }

    /**
     * Muestra el formulario para editar una categoría existente.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") String id,
                                          RedirectAttributes redirectAttributes,
                                          Model model) {
        try {
            Categoria categoria = categoriaService.buscarCategoria(id);
            model.addAttribute("categoria", categoria);
            model.addAttribute("titulo", "Zero | Editar Categoría");
            return "categorias/editar";
        } catch (NoSuchElementException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categorias";
        }
    }

    /**
     * Procesa la modificación del nombre de una categoría.
     */
    @PostMapping("/editar/{id}")
    public String editar(@PathVariable("id") String id,
                         @RequestParam("nombre") String nombre,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            categoriaService.modificarCategoria(id, nombre);
            redirectAttributes.addFlashAttribute("exito", "Categoría modificada con éxito.");
            return "redirect:/categorias";
        } catch (IllegalArgumentException e) {
            Categoria categoria = new Categoria(id, nombre, false);
            model.addAttribute("categoria", categoria);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("titulo", "Zero | Editar Categoría");
            return "categorias/editar";
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categorias";
        }
    }

    /**
     * Procesa la baja lógica de la categoría.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") String id,
                           RedirectAttributes redirectAttributes) {
        try {
            categoriaService.eliminarCategoria(id);
            redirectAttributes.addFlashAttribute("exito", "Categoría dada de baja correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categorias";
    }

    /**
     * Procesa la reactivación de una categoría dada de baja.
     */
    @PostMapping("/reactivar/{id}")
    public String reactivar(@PathVariable("id") String id,
                            RedirectAttributes redirectAttributes) {
        try {
            categoriaService.reactivarCategoria(id);
            redirectAttributes.addFlashAttribute("exito", "Categoría reactivada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categorias?mostrarEliminadas=true";
    }
}
