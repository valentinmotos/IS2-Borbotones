package com.borbotones.biblioteca.repo;
import com.borbotones.biblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByMail(String mail);
}