package com.example.servidor.service;

import com.example.servidor.dao.LibroDAO;
import com.example.servidor.model.Libro;
import com.example.servidor.patterns.behavioral.LibroIterator;
import com.example.servidor.patterns.behavioral.LibroSearchStrategy;
import com.example.servidor.patterns.creational.LibroFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibroService {
    private final LibroDAO libroDAO;
    private final PersonaService personaService;
    private final LibroFactory libroFactory;
    private final java.util.Map<String, LibroSearchStrategy> estrategias;

    public LibroService(LibroDAO libroDAO, PersonaService personaService, LibroFactory libroFactory,
                        java.util.Map<String, LibroSearchStrategy> estrategias) {
        this.libroDAO = libroDAO;
        this.personaService = personaService;
        this.libroFactory = libroFactory;
        this.estrategias = estrategias;
    }

    public List<Libro> listar() {
        return libroDAO.findAll();
    }

    public Libro buscar(Long id) {
        return libroDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("Libro no encontrado"));
    }

    public Libro guardar(Libro libro) {
        if (libro.getPersona() != null && libro.getPersona().getId() != null) {
            libro.setPersona(personaService.buscar(libro.getPersona().getId()));
        }
        return libroDAO.save(libro);
    }

    public Libro crearFisico(String titulo, Integer fecha, String genero, Integer paginas,
                             String autor, String formato) {
        return guardar(libroFactory.crearFisico(titulo, fecha, genero, paginas, autor, formato));
    }

    public Libro crearDigital(String titulo, Integer fecha, String genero, Integer paginas,
                              String autor, String formato, String urlDescarga) {
        return guardar(libroFactory.crearDigital(titulo, fecha, genero, paginas, autor, formato, urlDescarga));
    }

    public List<Libro> buscar(String criterio, String valor) {
        LibroSearchStrategy estrategia = estrategias.get(criterio == null ? "" : criterio.toLowerCase());
        if (estrategia == null) throw new IllegalArgumentException("Criterio inválido. Use titulo, genero o autor");
        return estrategia.buscar(listar(), valor);
    }

    public List<Libro> listarPorAutor(String autor) {
        List<Libro> resultado = new java.util.ArrayList<>();
        new LibroIterator(listar(), autor).forEachRemaining(resultado::add);
        return resultado;
    }

    public void eliminar(Long id) {
        libroDAO.deleteById(id);
    }
}
