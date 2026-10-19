package com.tp2.servidor.dao;

import com.tp2.servidor.entity.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagenDao extends JpaRepository<Imagen, String> {
}
