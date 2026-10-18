package com.example.servidor.patterns.behavioral;

import com.example.servidor.model.Libro;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/** Iterador que expone solo los libros de un autor. */
public class LibroIterator implements Iterator<Libro>, Iterable<Libro> {
    private final List<Libro> libros;
    private final String autor;
    private int indice;
    private Libro siguiente;
    private boolean preparado;

    public LibroIterator(List<Libro> libros, String autor) {
        this.libros = libros;
        this.autor = autor;
    }

    @Override public boolean hasNext() {
        prepararSiguiente();
        return siguiente != null;
    }

    @Override public Libro next() {
        prepararSiguiente();
        if (siguiente == null) throw new NoSuchElementException();
        Libro resultado = siguiente;
        siguiente = null;
        preparado = false;
        return resultado;
    }

    @Override public Iterator<Libro> iterator() { return this; }

    private void prepararSiguiente() {
        if (preparado) return;
        while (indice < libros.size()) {
            Libro candidato = libros.get(indice++);
            if (candidato.getAutor() != null && autor != null && candidato.getAutor().equalsIgnoreCase(autor)) {
                siguiente = candidato;
                preparado = true;
                return;
            }
        }
        preparado = true;
    }
}
