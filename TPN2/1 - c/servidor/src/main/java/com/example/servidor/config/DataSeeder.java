package com.example.servidor.config;

import com.example.servidor.model.Libro;
import com.example.servidor.model.Localidad;
import com.example.servidor.service.LibroService;
import com.example.servidor.service.LocalidadService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final LocalidadService localidadService;
    private final LibroService libroService;

    public DataSeeder(LocalidadService localidadService, LibroService libroService) {
        this.localidadService = localidadService;
        this.libroService = libroService;
    }

    @Override
    public void run(String... args) {
        crearLocalidad("Mendoza");
        crearLocalidad("Godoy Cruz");
        crearLocalidad("Guaymallen");
        crearLocalidad("Las Heras");

        if (libroService.listar().isEmpty()) {
            crearLibro("El principito", 1943, "Fabula", 96, "Antoine de Saint-Exupery");
            crearLibro("Cien anos de soledad", 1967, "Realismo magico", 471, "Gabriel Garcia Marquez");
            crearLibro("Rayuela", 1963, "Novela", 736, "Julio Cortazar");
        }
    }

    private void crearLocalidad(String denominacion) {
        if (!localidadService.existeDenominacion(denominacion)) {
            Localidad localidad = new Localidad();
            localidad.setDenominacion(denominacion);
            localidadService.guardar(localidad);
        }
    }

    private void crearLibro(String titulo, Integer fecha, String genero, Integer paginas, String autor) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setFecha(fecha);
        libro.setGenero(genero);
        libro.setPaginas(paginas);
        libro.setAutor(autor);
        libroService.guardar(libro);
    }
}
