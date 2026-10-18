package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Categoria;
import com.borbotones.videojuegos.services.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/categorias")
public class CategoriaController extends BaseControllerTemplate<Categoria> {
    public CategoriaController(CategoriaService service) { super(service); }

    @Override protected Categoria nuevaEntidad() { return new Categoria(); }
    @Override protected String nombreAtributo() { return "categoria"; }
    @Override protected String nombreColeccion() { return "categorias"; }
    @Override protected String vistaListado() { return "view/categoria/categoriaList"; }
    @Override protected String vistaFormulario() { return "view/categoria/categoriaEdit"; }
    @Override protected String redireccionListado() { return "redirect:/categorias"; }
    @Override protected long obtenerId(Categoria entity) { return entity.getId(); }
}
