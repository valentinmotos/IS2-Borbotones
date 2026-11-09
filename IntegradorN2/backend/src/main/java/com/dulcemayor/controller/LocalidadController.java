package com.dulcemayor.controller;

import com.dulcemayor.dto.LocalidadRequest;
import com.dulcemayor.dto.LocalidadResponse;
import com.dulcemayor.service.LocalidadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/localidades")
public class LocalidadController extends AbstractCrudController<LocalidadRequest, LocalidadResponse> {

    private final LocalidadService localidadService;

    public LocalidadController(LocalidadService localidadService) {
        super(localidadService);
        this.localidadService = localidadService;
    }

    @GetMapping("/codigo-postal/{codigoPostal}")
    public LocalidadResponse buscarPorCodigoPostal(@PathVariable String codigoPostal) {
        return localidadService.buscarLocalidadPorCodigoPostal(codigoPostal);
    }
}
