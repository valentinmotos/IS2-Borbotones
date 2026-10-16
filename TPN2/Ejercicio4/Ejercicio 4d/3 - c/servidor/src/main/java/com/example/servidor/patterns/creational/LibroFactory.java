package com.example.servidor.patterns.creational;

import com.example.servidor.model.Libro;
import org.springframework.stereotype.Component;

@Component
public class LibroFactory {
    public Libro crearFisico(String titulo, Integer fecha, String genero, Integer paginas,
                             String autor, String formato) {
        return Libro.builder().titulo(titulo).fecha(fecha).genero(genero).paginas(paginas)
                .autor(autor).tipo("FISICO").formato(formato).build();
    }

    public Libro crearDigital(String titulo, Integer fecha, String genero, Integer paginas,
                              String autor, String formato, String urlDescarga) {
        if (urlDescarga == null || urlDescarga.isBlank()) {
            throw new IllegalArgumentException("La URL de descarga es obligatoria para un libro digital");
        }
        return Libro.builder().titulo(titulo).fecha(fecha).genero(genero).paginas(paginas)
                .autor(autor).tipo("DIGITAL").formato(formato).urlDescarga(urlDescarga).build();
    }
}
