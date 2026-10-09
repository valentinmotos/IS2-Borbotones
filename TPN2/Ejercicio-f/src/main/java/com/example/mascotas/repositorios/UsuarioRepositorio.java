package com.example.mascotas.repositorios;
import com.example.mascotas.entidades.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsuarioRepositorio extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByMail(String mail);
}
