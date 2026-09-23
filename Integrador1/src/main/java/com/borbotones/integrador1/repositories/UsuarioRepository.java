package com.borbotones.integrador1.repositories;

import com.borbotones.integrador1.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

    Optional<Usuario> findByNombreUsuarioIgnoreCaseAndEliminadoFalse(String nombreUsuario);

    List<Usuario> findByEliminadoFalseOrderByNombreUsuarioAsc();
}
