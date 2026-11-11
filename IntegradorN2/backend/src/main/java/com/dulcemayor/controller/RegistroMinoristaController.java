package com.dulcemayor.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.dulcemayor.dto.ActivarCuentaRequest;
import com.dulcemayor.dto.CorreoRequest;
import com.dulcemayor.dto.MensajeResponse;
import com.dulcemayor.dto.RegistroMinoristaRequest;
import com.dulcemayor.service.RegistroMinoristaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/publico/registro")
public class RegistroMinoristaController {

    private final RegistroMinoristaService registroService;

    public RegistroMinoristaController(RegistroMinoristaService registroService) {
        this.registroService = registroService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MensajeResponse registrar(@Valid @RequestBody RegistroMinoristaRequest request) {
        return new MensajeResponse(registroService.registrar(request));
    }

    @PostMapping("/activar")
    public MensajeResponse activar(@Valid @RequestBody ActivarCuentaRequest request) {
        return new MensajeResponse(registroService.activar(request));
    }

    @PostMapping("/reenviar")
    public MensajeResponse reenviar(@Valid @RequestBody CorreoRequest request) {
        return new MensajeResponse(registroService.reenviar(request.correo()));
    }
}
