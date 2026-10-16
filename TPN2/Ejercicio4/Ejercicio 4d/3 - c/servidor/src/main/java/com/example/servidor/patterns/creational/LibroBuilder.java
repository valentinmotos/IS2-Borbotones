package com.example.servidor.patterns.creational;

import com.example.servidor.model.Libro;

/** Builder para construir libros con sus datos principales. */
public class LibroBuilder {
    private final Libro libro = new Libro();

    public LibroBuilder titulo(String titulo) { libro.setTitulo(titulo); return this; }
    public LibroBuilder fecha(Integer fecha) { libro.setFecha(fecha); return this; }
    public LibroBuilder genero(String genero) { libro.setGenero(genero); return this; }
    public LibroBuilder paginas(Integer paginas) { libro.setPaginas(paginas); return this; }
    public LibroBuilder autor(String autor) { libro.setAutor(autor); return this; }
    public LibroBuilder tipo(String tipo) { libro.setTipo(tipo); return this; }
    public LibroBuilder formato(String formato) { libro.setFormato(formato); return this; }
    public LibroBuilder urlDescarga(String url) { libro.setUrlDescarga(url); return this; }

    public Libro build() {
        if (libro.getTitulo() == null || libro.getTitulo().isBlank()) {
            throw new IllegalStateException("El título del libro es obligatorio");
        }
        return libro;
    }
}
