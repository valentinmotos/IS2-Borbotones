package com.example.servidor.controller;

import com.example.servidor.service.MigracionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/migracion")
public class MigracionController {
    private final MigracionService migracionService;

    public MigracionController(MigracionService migracionService) {
        this.migracionService = migracionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public int importar(@RequestPart("archivo") MultipartFile archivo) throws IOException {
        if (archivo.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Seleccioná un archivo TXT con proveedores.");
        }
        return migracionService.importar(archivo.getBytes());
    }
}
