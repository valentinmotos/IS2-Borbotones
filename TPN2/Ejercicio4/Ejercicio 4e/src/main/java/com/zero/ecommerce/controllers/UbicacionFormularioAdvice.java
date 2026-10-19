package com.zero.ecommerce.controllers;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.zero.ecommerce.controllers.admin.EmpresaController;
import com.zero.ecommerce.controllers.admin.LocalidadController;
import com.zero.ecommerce.controllers.cliente.ClienteController;
import com.zero.ecommerce.dto.OpcionUbicacionDTO;
import com.zero.ecommerce.services.DepartamentoService;
import com.zero.ecommerce.services.LocalidadService;
import com.zero.ecommerce.services.PaisService;
import com.zero.ecommerce.services.ProvinciaService;

/** Ofrece al HTML las opciones de ubicación sin solicitudes JSON desde el navegador. */
@ControllerAdvice(assignableTypes = {ClienteController.class, EmpresaController.class,
        LocalidadController.class, DevController.class})
public class UbicacionFormularioAdvice {

    private final PaisService paisService;
    private final ProvinciaService provinciaService;
    private final DepartamentoService departamentoService;
    private final LocalidadService localidadService;

    public UbicacionFormularioAdvice(PaisService paisService, ProvinciaService provinciaService,
            DepartamentoService departamentoService, LocalidadService localidadService) {
        this.paisService = paisService;
        this.provinciaService = provinciaService;
        this.departamentoService = departamentoService;
        this.localidadService = localidadService;
    }

    @ModelAttribute
    public void agregarOpciones(Model model) {
        model.addAttribute("ubicacionPaises", paisService.listarPaisActivo().stream()
                .map(p -> new OpcionUbicacionDTO(p.getId(), p.getNombre(), null, null)).toList());
        model.addAttribute("ubicacionProvincias", provinciaService.listarProvinciaActivo(null).stream()
                .map(p -> new OpcionUbicacionDTO(p.getId(), p.getNombre(), p.getPais().getId(), null)).toList());
        model.addAttribute("ubicacionDepartamentos", departamentoService.listarDepartamentoActivo(null).stream()
                .map(d -> new OpcionUbicacionDTO(d.getId(), d.getNombre(), d.getProvincia().getId(), null)).toList());
        model.addAttribute("ubicacionLocalidades", localidadService.listarLocalidadActivo(null).stream()
                .map(l -> new OpcionUbicacionDTO(l.getId(), l.getNombre(), l.getDepartamento().getId(),
                        l.getCodigoPostal())).toList());
    }
}
