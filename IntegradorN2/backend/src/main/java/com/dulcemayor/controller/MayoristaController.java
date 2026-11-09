package com.dulcemayor.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dulcemayor.dto.MayoristaRequest;
import com.dulcemayor.dto.MayoristaResponse;
import com.dulcemayor.service.MayoristaService;

@RestController
@RequestMapping("/api/empresas")
public class MayoristaController extends AbstractCrudController<MayoristaRequest, MayoristaResponse> {
    public MayoristaController(MayoristaService servicio) { super(servicio); }
}
