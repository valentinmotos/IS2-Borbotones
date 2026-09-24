package com.zero.ecommerce;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) throws IOException {
        // sqlite-jdbc no crea directorios: la carpeta de la base tiene que existir antes de levantar el datasource.
        Files.createDirectories(Path.of("data"));
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
