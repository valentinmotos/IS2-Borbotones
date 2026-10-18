package com.tp2.servidor.dao;

import com.tp2.servidor.entity.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticuloDao extends JpaRepository<Articulo, String> {
}
