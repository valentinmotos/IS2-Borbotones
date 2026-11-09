package com.dulcemayor.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dulcemayor.dto.SucursalRequest;
import com.dulcemayor.dto.SucursalResponse;
import com.dulcemayor.service.SucursalService;

@RestController
@RequestMapping("/api/sucursales")
public class SucursalController extends AbstractCrudController<SucursalRequest, SucursalResponse> {
    private final SucursalService sucursalService;
    public SucursalController(SucursalService sucursalService) { super(sucursalService); this.sucursalService = sucursalService; }

    @GetMapping("/empresa/{idMayorista}")
    public List<SucursalResponse> listarPorEmpresa(@PathVariable String idMayorista) {
        return sucursalService.listarSucursalActivo(idMayorista);
    }
}
