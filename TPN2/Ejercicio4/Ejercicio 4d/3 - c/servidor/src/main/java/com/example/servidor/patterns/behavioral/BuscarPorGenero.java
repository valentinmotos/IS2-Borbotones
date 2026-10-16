package com.example.servidor.patterns.behavioral;

import com.example.servidor.model.Libro;
import org.springframework.stereotype.Component;
import java.util.List;

@Component("genero")
public class BuscarPorGenero implements LibroSearchStrategy {
    public List<Libro> buscar(List<Libro> libros, String criterio) {
        return libros.stream().filter(l -> l.getGenero() != null && criterio != null
                && l.getGenero().equalsIgnoreCase(criterio)).toList();
    }
}
