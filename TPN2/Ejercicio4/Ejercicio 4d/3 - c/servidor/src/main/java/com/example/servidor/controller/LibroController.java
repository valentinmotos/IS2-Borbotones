package com.example.servidor.controller;

import com.example.servidor.model.Libro;
import com.example.servidor.dto.LibroDTO;
import com.example.servidor.mapper.LibroMapper;
import com.example.servidor.service.LibroService;
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
@RequestMapping("/api/libros")
public class LibroController {
    private final LibroService libroService;
    private final LibroMapper libroMapper;

    public LibroController(LibroService libroService, LibroMapper libroMapper) {
        this.libroService = libroService;
        this.libroMapper = libroMapper;
    }

    @GetMapping
    public List<Libro> listar() {
        return libroService.listar();
    }

    @GetMapping("/buscar")
    public List<LibroDTO> buscarPor(@org.springframework.web.bind.annotation.RequestParam String criterio,
                                    @org.springframework.web.bind.annotation.RequestParam String valor) {
        return libroService.buscar(criterio, valor).stream().map(libroMapper::toDto).toList();
    }

    @GetMapping("/autor/{autor}")
    public List<LibroDTO> listarPorAutor(@PathVariable String autor) {
        return libroService.listarPorAutor(autor).stream().map(libroMapper::toDto).toList();
    }

    public record CrearLibroRequest(String titulo, Integer fecha, String genero, Integer paginas,
                                    String autor, String formato, String urlDescarga) {}

    @PostMapping("/fisico")
    public LibroDTO crearFisico(@RequestBody CrearLibroRequest request) {
        return libroMapper.toDto(libroService.crearFisico(request.titulo(), request.fecha(), request.genero(),
                request.paginas(), request.autor(), request.formato()));
    }

    @PostMapping("/digital")
    public LibroDTO crearDigital(@RequestBody CrearLibroRequest request) {
        return libroMapper.toDto(libroService.crearDigital(request.titulo(), request.fecha(), request.genero(),
                request.paginas(), request.autor(), request.formato(), request.urlDescarga()));
    }

    @GetMapping("/{id}")
    public Libro buscar(@PathVariable Long id) {
        return libroService.buscar(id);
    }

    @PostMapping
    public Libro crear(@RequestBody Libro libro) {
        return libroService.guardar(libro);
    }

    @PutMapping("/{id}")
    public Libro actualizar(@PathVariable Long id, @RequestBody Libro libro) {
        libro.setId(id);
        return libroService.guardar(libro);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
    }
}
