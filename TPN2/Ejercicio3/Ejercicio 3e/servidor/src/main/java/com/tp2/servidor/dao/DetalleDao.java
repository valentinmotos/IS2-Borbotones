package com.tp2.servidor.dao;

import com.tp2.servidor.entity.Detalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleDao extends JpaRepository<Detalle, String> {
}
