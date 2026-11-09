package com.dulcemayor.controller;

import com.dulcemayor.dto.ImagenContenidoDTO;
import com.dulcemayor.dto.ImagenCargaDTO;
import com.dulcemayor.dto.ImagenDTO;
import com.dulcemayor.service.ImagenService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/imagenes")
public class ImagenController extends AbstractCrudController<ImagenCargaDTO, ImagenDTO> {

    private final ImagenService imagenService;

    public ImagenController(ImagenService imagenService) {
        super(imagenService);
        this.imagenService = imagenService;
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> buscar(@PathVariable String id) {
        ImagenContenidoDTO imagen = imagenService.buscarContenido(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, imagen.mime())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(imagen.contenido());
    }

    // Metodos no Funcionales, solo para no generar error de Services
    // ------------------------------------------------------>>>>>>>>
    @Override
    @GetMapping
    public List<ImagenDTO> listar() {
        return List.of();
    }

    @Override
    @PostMapping
    public ImagenDTO crear(@RequestBody(required = false) ImagenCargaDTO request) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Override
    @PutMapping("/{id}")
    public ImagenDTO modificar(@PathVariable String id, @RequestBody(required = false) ImagenCargaDTO request) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
    }
    // ------------------------------------------------------>>>>>>>>
    // ------------------------------------------------------>>>>>>>>
}
