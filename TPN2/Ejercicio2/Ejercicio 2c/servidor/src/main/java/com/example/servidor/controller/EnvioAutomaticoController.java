package com.example.servidor.controller;

import com.example.servidor.model.EnvioAutomatico;
import com.example.servidor.service.EnvioAutomaticoService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/envios-automaticos")
public class EnvioAutomaticoController {
    private final EnvioAutomaticoService envioAutomaticoService;

    public EnvioAutomaticoController(EnvioAutomaticoService envioAutomaticoService) {
        this.envioAutomaticoService = envioAutomaticoService;
    }

    @GetMapping
    public List<EnvioAutomatico> listar() {
        return envioAutomaticoService.listar();
    }

    @GetMapping("/{id}")
    public EnvioAutomatico buscar(@PathVariable Long id) {
        return envioAutomaticoService.buscar(id);
    }

    @PostMapping
    public EnvioAutomatico crear(@RequestBody EnvioAutomatico envioAutomatico) {
        return envioAutomaticoService.guardar(envioAutomatico);
    }

    @PutMapping("/{id}")
    public EnvioAutomatico actualizar(@PathVariable Long id, @RequestBody EnvioAutomatico envioAutomatico) {
        envioAutomatico.setId(id);
        return envioAutomaticoService.guardar(envioAutomatico);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        envioAutomaticoService.eliminar(id);
    }
}
