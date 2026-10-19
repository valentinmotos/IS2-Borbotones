package com.example.servidor.dao;

import com.example.servidor.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutorDAO extends JpaRepository<Autor, Long> {
}
