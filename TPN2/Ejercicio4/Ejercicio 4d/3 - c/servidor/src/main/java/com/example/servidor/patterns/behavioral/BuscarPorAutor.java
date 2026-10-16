package com.example.servidor.patterns.behavioral;

import com.example.servidor.model.Libro;
import org.springframework.stereotype.Component;
import java.util.List;

@Component("autor")
public class BuscarPorAutor implements LibroSearchStrategy {
    public List<Libro> buscar(List<Libro> libros, String criterio) {
        return libros.stream().filter(l -> l.getAutor() != null && criterio != null
                && l.getAutor().toLowerCase().contains(criterio.toLowerCase())).toList();
    }
}
