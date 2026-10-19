package com.example.cliente.controller;

import com.example.cliente.dto.BaseDTO;
import com.example.cliente.service.BaseService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public abstract class BaseController<D extends BaseDTO> {
    protected final BaseService<D> service;
    private final String carpeta;
    private final String nombreLista;
    private final String nombreObjeto;

    protected BaseController(BaseService<D> service, String carpeta, String nombreLista, String nombreObjeto) {
        this.service = service;
        this.carpeta = carpeta;
        this.nombreLista = nombreLista;
        this.nombreObjeto = nombreObjeto;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute(nombreLista, service.listar());
        return carpeta + "/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return mostrarFormulario(model, service.nuevo());
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        return mostrarFormulario(model, service.buscar(id));
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute D dto) {
        service.guardar(dto);
        return "redirect:/" + carpeta;
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return "redirect:/" + carpeta;
    }

    protected final String mostrarFormulario(Model model, D dto) {
        model.addAttribute(nombreObjeto, dto);
        cargarDatosFormulario(model);
        return carpeta + "/formulario";
    }

    protected void cargarDatosFormulario(Model model) {
    }
}
