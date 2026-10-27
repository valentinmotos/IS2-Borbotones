package com.example.servidor.dao;

import com.example.servidor.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorDAO extends JpaRepository<Proveedor, Long> {
}
