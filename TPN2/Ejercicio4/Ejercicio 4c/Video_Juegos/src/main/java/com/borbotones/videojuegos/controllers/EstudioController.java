package com.borbotones.videojuegos.controllers;

import com.borbotones.videojuegos.entities.Estudio;
import com.borbotones.videojuegos.services.EstudioService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/estudios")
public class EstudioController extends BaseControllerTemplate<Estudio> {
    public EstudioController(EstudioService service) { super(service); }

    @Override protected Estudio nuevaEntidad() { return new Estudio(); }
    @Override protected String nombreAtributo() { return "estudio"; }
    @Override protected String nombreColeccion() { return "estudios"; }
    @Override protected String vistaListado() { return "view/estudio/estudioList"; }
    @Override protected String vistaFormulario() { return "view/estudio/estudioEdit"; }
    @Override protected String redireccionListado() { return "redirect:/estudios"; }
    @Override protected long obtenerId(Estudio entity) { return entity.getId(); }
}
