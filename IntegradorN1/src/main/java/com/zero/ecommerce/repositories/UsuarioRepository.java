package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
}
