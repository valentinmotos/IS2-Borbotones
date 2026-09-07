package com.example.mascotas.repositorios;

import com.example.mascotas.entidades.Foto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FotoRepositorio extends JpaRepository<Foto, String>, RevisionRepository<Foto, String, Integer> {

}
