package com.tp2.servidor.dao;

import com.tp2.servidor.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorDao extends JpaRepository<Proveedor, String> {
}
