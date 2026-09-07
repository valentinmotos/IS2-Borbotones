package com.borbotones.biblioteca.config;

import com.borbotones.biblioteca.model.*;
import com.borbotones.biblioteca.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean CommandLineRunner seed(UsuarioRepository usuarios, AutorRepository autores,
                                  EditorialRepository editoriales, LibroRepository libros,
                                  PasswordEncoder encoder) {
        return args -> {
            if (usuarios.count() == 0) {
                Usuario admin = new Usuario(); admin.setDni("00000000"); admin.setNombre("Administrador");
                admin.setMail("admin@biblioteca.local"); admin.setClave(encoder.encode("admin123")); admin.setRol(Rol.ADMIN);
                usuarios.save(admin);
                Usuario user = new Usuario(); user.setDni("11111111"); user.setNombre("Lector demo");
                user.setMail("lector@biblioteca.local"); user.setClave(encoder.encode("lector123")); usuarios.save(user);
            }
            if (autores.count() == 0) {
                Autor autor = new Autor(); autor.setNombre("Gabriel Garcia Marquez"); autores.save(autor);
                Editorial editorial = new Editorial(); editorial.setNombre("Editorial Sudamericana"); editoriales.save(editorial);
                Libro libro = new Libro(); libro.setIsbn("978-0307474728"); libro.setTitulo("Cien anos de soledad");
                libro.setAnio(1967); libro.setEjemplares(4); libro.setAutor(autor); libro.setEditorial(editorial); libros.save(libro);
            }
        };
    }
}