package com.dulcemayor.controller;

import com.dulcemayor.dto.DepartamentoRequest;
import com.dulcemayor.dto.DepartamentoResponse;
import com.dulcemayor.dto.LocalidadResponse;
import com.dulcemayor.service.DepartamentoService;
import com.dulcemayor.service.LocalidadService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departamentos")
public class DepartamentoController extends AbstractCrudController<DepartamentoRequest, DepartamentoResponse> {

    private final LocalidadService localidadService;

    public DepartamentoController(DepartamentoService departamentoService, LocalidadService localidadService) {
        super(departamentoService);
        this.localidadService = localidadService;
    }

    @GetMapping("/{id}/localidades")
    public List<LocalidadResponse> listarLocalidades(@PathVariable String id) {
        return localidadService.listarLocalidad(id);
    }
}
