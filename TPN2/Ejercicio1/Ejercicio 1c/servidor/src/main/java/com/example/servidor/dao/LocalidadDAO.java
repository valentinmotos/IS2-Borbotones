package com.example.servidor.dao;

import com.example.servidor.model.Localidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalidadDAO extends JpaRepository<Localidad, Long> {
    boolean existsByDenominacion(String denominacion);
}
