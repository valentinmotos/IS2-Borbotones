package com.example.mascotas.repositorios;
import com.example.mascotas.entidades.Foto;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FotoRepositorio extends JpaRepository<Foto, String> { }
