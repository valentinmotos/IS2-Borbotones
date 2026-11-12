package com.dulcemayor.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.PromocionRequest;
import com.dulcemayor.dto.PromocionResponse;
import com.dulcemayor.service.PromocionService;

/** E4-03: ABM interno de códigos de promoción. */
@RestController
@RequestMapping("/api/promociones")
public class PromocionController extends AbstractCrudController<PromocionRequest, PromocionResponse> {

    private final PromocionService promociones;

    public PromocionController(PromocionService promociones) {
        super(promociones);
        this.promociones = promociones;
    }

    @Override
    @GetMapping
    public List<PromocionResponse> listar() {
        return promociones.listarPromociones();
    }
}
