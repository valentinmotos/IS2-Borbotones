package com.example.mascotas.repositorios;

import com.example.mascotas.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByOauthIssuerAndOauthSubject(String issuer, String subject);
}
