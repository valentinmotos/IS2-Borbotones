package com.example.servidor.controller;

import com.example.servidor.model.Libro;
import com.example.servidor.service.LibroService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
public class LibroController {
    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    public List<Libro> listar() {
        return libroService.listar();
    }

    @GetMapping("/{id}")
    public Libro buscar(@PathVariable Long id) {
        return libroService.buscar(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Libro crear(@RequestBody Libro libro) {
        libro.setId(null);
        return libroService.guardar(libro);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Libro crearConPdf(@RequestPart("libro") Libro libro,
                             @RequestPart(value = "pdf", required = false) MultipartFile pdf) {
        libro.setId(null);
        return libroService.guardar(libro, pdf);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Libro actualizarConPdf(@PathVariable Long id, @RequestPart("libro") Libro libro,
                                  @RequestPart(value = "pdf", required = false) MultipartFile pdf) {
        libro.setId(id);
        return libroService.guardar(libro, pdf);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> abrirPdf(@PathVariable Long id) {
        Resource pdf = libroService.abrirPdf(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.getFilename()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(pdf);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Libro actualizar(@PathVariable Long id, @RequestBody Libro libro) {
        libro.setId(id);
        return libroService.guardar(libro);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
    }
}
