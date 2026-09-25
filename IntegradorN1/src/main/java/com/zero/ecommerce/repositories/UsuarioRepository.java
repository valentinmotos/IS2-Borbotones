package com.zero.ecommerce.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);
}
