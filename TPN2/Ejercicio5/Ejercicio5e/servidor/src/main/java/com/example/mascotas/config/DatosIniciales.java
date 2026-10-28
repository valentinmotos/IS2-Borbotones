package com.example.mascotas.config;

import com.example.mascotas.entidades.Zona;
import com.example.mascotas.repositorios.ZonaRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatosIniciales {
    @Bean
    CommandLineRunner zonasIniciales(ZonaRepositorio zonas) {
        return args -> {
            if (zonas.count() == 0) {
                for (String nombre : new String[]{"Centro", "Norte", "Sur", "Este", "Oeste"}) {
                    Zona zona = new Zona();
                    zona.setNombre(nombre);
                    zona.setDescripcion("Zona " + nombre);
                    zonas.save(zona);
                }
            }
        };
    }
}
