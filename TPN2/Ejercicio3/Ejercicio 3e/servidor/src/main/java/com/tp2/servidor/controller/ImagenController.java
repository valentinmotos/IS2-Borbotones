package com.tp2.servidor.controller;

import com.tp2.servidor.dto.ImagenRequest;
import com.tp2.servidor.entity.Imagen;
import com.tp2.servidor.service.ImagenService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/imagenes")
public class ImagenController extends BaseRestController<Imagen, ImagenRequest, String> {
    public ImagenController(ImagenService service) {
        super(service);
    }

    @GetMapping("/{id}/contenido")
    public ResponseEntity<byte[]> contenido(@PathVariable String id) {
        return baseService.buscarPorId(id)
                .map(imagen -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(imagen.getMime()))
                        .body(imagen.getContenido()))
                .orElse(ResponseEntity.notFound().build());
    }
}
