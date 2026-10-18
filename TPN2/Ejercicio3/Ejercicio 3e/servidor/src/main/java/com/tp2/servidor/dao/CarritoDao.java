package com.tp2.servidor.dao;

import com.tp2.servidor.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarritoDao extends JpaRepository<Carrito, String> {
}
