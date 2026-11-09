package com.dulcemayor.controller;

import com.dulcemayor.dto.ImagenContenidoDTO;
import com.dulcemayor.dto.ImagenDTO;
import com.dulcemayor.dto.ImagenCargaDTO;
import com.dulcemayor.service.ImagenService;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/imagenes")
public class ImagenController {

    private final ImagenService imagenService;

    public ImagenController(ImagenService imagenService) {
        this.imagenService = imagenService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> buscar(@PathVariable String id) {
        ImagenContenidoDTO imagen = imagenService.buscarContenido(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, imagen.mime())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(imagen.contenido());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImagenDTO crear(@ModelAttribute ImagenCargaDTO request) {
        return imagenService.crear(request);
    }
}
