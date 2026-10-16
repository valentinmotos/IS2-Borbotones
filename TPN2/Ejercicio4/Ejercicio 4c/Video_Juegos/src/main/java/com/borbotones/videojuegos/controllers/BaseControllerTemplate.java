package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.services.BaseService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Plantilla MVC para operaciones CRUD que comparten el flujo de carga del
 * modelo, acceso al servicio, redirección y presentación de errores.
 */
public abstract class BaseControllerTemplate<E> {

    protected final BaseService<E> baseService;

    protected BaseControllerTemplate(BaseService<E> baseService) {
        this.baseService = baseService;
    }

    @GetMapping
    public String listar(Model model) {
        try {
            model.addAttribute(nombreColeccion(), baseService.findAll());
            prepararModeloListado(model);
            return vistaListado();
        } catch (Exception e) {
            return mostrarError(model, e);
        }
    }

    @GetMapping("/{recurso}/")
    public String listarPorRecurso(Model model) { return listar(model); }

    @GetMapping("/alta")
    public String prepararAlta(Model model) {
        model.addAttribute(nombreAtributo(), nuevaEntidad());
        return prepararFormulario(model, "alta");
    }

    @PostMapping("/alta")
    public String alta(@ModelAttribute("formObject") E entity, Model model) {
        try {
            baseService.saveOne(entity);
            return redireccionListado();
        } catch (Exception e) {
            return mostrarError(model, e);
        }
    }

    @GetMapping("/modificar")
    public String prepararModificacion(@RequestParam("id") long id, Model model) {
        return prepararConsulta(id, model, "modificar");
    }

    @PostMapping("/modificar")
    public String modificar(@ModelAttribute("formObject") E entity, Model model) {
        try {
            long id = obtenerId(entity);
            baseService.updateOne(entity, id);
            return redireccionListado();
        } catch (Exception e) {
            return mostrarError(model, e);
        }
    }

    @GetMapping("/consultar")
    public String prepararConsulta(@RequestParam("id") long id, Model model) {
        return prepararConsulta(id, model, "consulta");
    }

    protected String prepararConsulta(long id, Model model, String modo) {
        try {
            model.addAttribute(nombreAtributo(), baseService.findById(id));
            return prepararFormulario(model, modo);
        } catch (Exception e) {
            return mostrarError(model, e);
        }
    }

    @GetMapping("/baja")
    public String baja(@RequestParam("id") long id, Model model) {
        try {
            baseService.deleteById(id);
            return redireccionListado();
        } catch (Exception e) {
            return mostrarError(model, e);
        }
    }

    protected String mostrarError(Model model, Exception exception) {
        model.addAttribute("error", exception.getMessage());
        return "error";
    }

    protected void prepararModeloFormulario(Model model, String modo) throws Exception {
        model.addAttribute("modo", modo);
    }

    protected String prepararFormulario(Model model, String modo) throws Exception {
        prepararModeloFormulario(model, modo);
        return vistaFormulario();
    }

    protected void prepararModeloListado(Model model) {}

    protected abstract E nuevaEntidad();
    protected abstract String nombreAtributo();
    protected abstract String nombreColeccion();
    protected abstract String vistaListado();
    protected abstract String vistaFormulario();
    protected abstract String redireccionListado();
}
