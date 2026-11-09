package com.dulcemayor.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.dulcemayor.dto.FormaDePagoRequest;
import com.dulcemayor.dto.FormaDePagoResponse;
import com.dulcemayor.service.FormaDePagoService;

@RestController
@RequestMapping("/api/formas-de-pago")
public class FormaDePagoController extends AbstractCrudController<FormaDePagoRequest, FormaDePagoResponse> {
    private final FormaDePagoService formaDePagoService;
    public FormaDePagoController(FormaDePagoService servicio) { super(servicio); this.formaDePagoService = servicio; }

    @GetMapping("/administracion")
    public List<FormaDePagoResponse> listarTodas() { return formaDePagoService.listarTodas(); }

    @PatchMapping("/{id}/activar")
    public FormaDePagoResponse activar(@PathVariable String id) { return formaDePagoService.activar(id); }
}
