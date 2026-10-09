package com.example.cliente.controller;

import com.example.cliente.service.MigracionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Controller
public class MigracionController {
    private final MigracionService migracionService;

    public MigracionController(MigracionService migracionService) {
        this.migracionService = migracionService;
    }

    @GetMapping("/migracion")
    public String formulario() {
        return "migracion/formulario";
    }

    @PostMapping("/migracion")
    public String importar(@RequestParam("archivo") MultipartFile archivo, Model model) throws IOException {
        if (archivo.isEmpty()) {
            model.addAttribute("error", "Seleccioná el archivo migracion.txt antes de continuar.");
            return "migracion/formulario";
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase(java.util.Locale.ROOT).endsWith(".txt")) {
            model.addAttribute("error", "El archivo debe tener formato .txt.");
            return "migracion/formulario";
        }
        try {
            model.addAttribute("cantidad", migracionService.importar(archivo));
        } catch (ResponseStatusException ex) {
            model.addAttribute("error", ex.getReason());
        } catch (RestClientResponseException ex) {
            model.addAttribute("error", "No se pudo importar el archivo. Revisá que cada renglón tenga los cinco campos completos y separados por punto y coma.");
        }
        return "migracion/formulario";
    }
}
