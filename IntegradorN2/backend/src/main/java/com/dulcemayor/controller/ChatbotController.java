package com.dulcemayor.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dulcemayor.dto.PresupuestoRequest;
import com.dulcemayor.dto.PresupuestoResponse;
import com.dulcemayor.service.PresupuestoService;

import jakarta.validation.Valid;

/**
 * Chatbot de presupuesto (E3-04). Público: lo usa el chat del sitio (E3-05) sin login, por eso va
 * bajo /api/publico (abierto en SecurityConfig). Responde 503 si Ollama no está disponible.
 */
@RestController
@RequestMapping("/api/publico/chatbot")
public class ChatbotController {

    private final PresupuestoService presupuestoService;

    public ChatbotController(PresupuestoService presupuestoService) {
        this.presupuestoService = presupuestoService;
    }

    @PostMapping("/presupuesto")
    public PresupuestoResponse armarPresupuesto(@Valid @RequestBody PresupuestoRequest request) {
        return presupuestoService.armarPresupuesto(request.mensaje());
    }
}
