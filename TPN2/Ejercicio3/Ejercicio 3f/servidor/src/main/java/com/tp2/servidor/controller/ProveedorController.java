package com.tp2.servidor.controller;

import com.tp2.servidor.dto.ProveedorRequest;
import com.tp2.servidor.entity.Proveedor;
import com.tp2.servidor.service.ProveedorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController extends BaseRestController<Proveedor, ProveedorRequest, String> {
    public ProveedorController(ProveedorService service) {
        super(service);
    }
}
