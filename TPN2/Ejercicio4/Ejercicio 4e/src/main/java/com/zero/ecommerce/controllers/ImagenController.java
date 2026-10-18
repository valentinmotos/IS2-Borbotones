package com.zero.ecommerce.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.zero.ecommerce.entities.Imagen;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.services.ImagenService;

@Controller
public class ImagenController {

    private final ImagenService imagenService;

    public ImagenController(ImagenService imagenService) {
        this.imagenService = imagenService;
    }

    @GetMapping("/imagen/{id}")
    public ResponseEntity<byte[]> mostrarImagen(@PathVariable String id) {
        try {
            Imagen imagen = imagenService.buscarImagen(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(imagen.getMime() != null ? imagen.getMime() : MediaType.IMAGE_JPEG_VALUE));
            headers.setCacheControl("no-store");
            return new ResponseEntity<>(imagen.getContenido(), headers, HttpStatus.OK);
        } catch (ErrorServiceException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
