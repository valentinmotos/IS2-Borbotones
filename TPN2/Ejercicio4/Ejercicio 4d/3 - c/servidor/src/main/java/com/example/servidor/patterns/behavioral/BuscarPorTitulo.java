package com.example.servidor.patterns.behavioral;

import com.example.servidor.model.Libro;
import org.springframework.stereotype.Component;
import java.util.List;

@Component("titulo")
public class BuscarPorTitulo implements LibroSearchStrategy {
    public List<Libro> buscar(List<Libro> libros, String criterio) {
        return libros.stream().filter(l -> contiene(l.getTitulo(), criterio)).toList();
    }
    private boolean contiene(String campo, String criterio) {
        return campo != null && criterio != null && campo.toLowerCase().contains(criterio.toLowerCase());
    }
}
