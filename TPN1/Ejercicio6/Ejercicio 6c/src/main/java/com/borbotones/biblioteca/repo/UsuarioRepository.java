package com.borbotones.biblioteca.repo;
import com.borbotones.biblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, RevisionRepository<Usuario, Long, Integer> {
    Optional<Usuario> findByMail(String mail);
}
