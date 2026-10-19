package com.tp2.servidor.dao;

import com.tp2.servidor.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioDao extends JpaRepository<Usuario, String> {
}
