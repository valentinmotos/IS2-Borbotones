package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Usuario;
import com.zero.ecommerce.entities.enums.RolUsuario;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

    Optional<Usuario> findByIdAndEliminadoFalse(String id);

    Optional<Usuario> findByNombreUsuarioIgnoreCaseAndEliminadoFalse(String nombreUsuario);

    List<Usuario> findAllByOrderByNombreUsuarioAsc();

    long countByRolAndEliminadoFalse(RolUsuario rol);
}
