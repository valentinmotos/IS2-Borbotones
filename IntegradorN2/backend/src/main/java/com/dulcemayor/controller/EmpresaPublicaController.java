package com.dulcemayor.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dulcemayor.dto.FormaDePagoResponse;
import com.dulcemayor.dto.MayoristaResponse;
import com.dulcemayor.dto.SucursalResponse;
import com.dulcemayor.service.FormaDePagoService;
import com.dulcemayor.service.MayoristaService;
import com.dulcemayor.service.SucursalService;

/** Datos activos que consume la vidriera institucional. */
@RestController
@RequestMapping("/api/publico")
public class EmpresaPublicaController {

    private final MayoristaService mayoristaService;
    private final SucursalService sucursalService;
    private final FormaDePagoService formaDePagoService;

    public EmpresaPublicaController(MayoristaService mayoristaService, SucursalService sucursalService,
            FormaDePagoService formaDePagoService) {
        this.mayoristaService = mayoristaService;
        this.sucursalService = sucursalService;
        this.formaDePagoService = formaDePagoService;
    }

    @GetMapping("/empresa")
    public List<MayoristaResponse> empresa() { return mayoristaService.listarMayoristaActivo(); }

    @GetMapping("/sucursales")
    public List<SucursalResponse> sucursales() { return sucursalService.listarSucursalActivo(); }

    @GetMapping("/formas-de-pago")
    public List<FormaDePagoResponse> formasDePago() { return formaDePagoService.listarFormaDePagoActivo(); }
}
