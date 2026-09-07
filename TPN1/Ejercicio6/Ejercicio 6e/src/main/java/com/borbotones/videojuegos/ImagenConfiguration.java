package com.borbotones.videojuegos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class ImagenConfiguration implements WebMvcConfigurer {
    private final Path directorioImagenes;

    public ImagenConfiguration(@Value("${app.imagenes.directorio}") String directorioImagenes) {
        this.directorioImagenes = Path.of(directorioImagenes).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = directorioImagenes.toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }
        registry.addResourceHandler("/imagenes/**").addResourceLocations(ubicacion);
    }
}
