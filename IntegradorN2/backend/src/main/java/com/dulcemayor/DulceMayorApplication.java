package com.dulcemayor;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST de Dulce Mayor.
 */
@SpringBootApplication
public class DulceMayorApplication {

    /** Zona horaria del negocio (Mendoza), igual en cualquier máquina o contenedor. */
    public static final String ZONA_HORARIA = "America/Argentina/Mendoza";

    public static void main(String[] args) {
        // El JVM de Windows en Argentina toma la zona "America/Buenos_Aires", un alias viejo que
        // PostgreSQL 16 rechaza al conectarse ("invalid value for parameter TimeZone").
        // Fijarla acá evita ese error y hace que LocalDateTime.now() sea siempre hora de Mendoza.
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
        SpringApplication.run(DulceMayorApplication.class, args);
    }
}
