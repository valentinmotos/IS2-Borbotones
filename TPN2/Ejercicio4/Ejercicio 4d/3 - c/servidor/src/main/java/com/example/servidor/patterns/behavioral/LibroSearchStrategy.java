package com.example.servidor.patterns.behavioral;

import com.example.servidor.model.Libro;
import java.util.List;

public interface LibroSearchStrategy {
    List<Libro> buscar(List<Libro> libros, String criterio);
}
