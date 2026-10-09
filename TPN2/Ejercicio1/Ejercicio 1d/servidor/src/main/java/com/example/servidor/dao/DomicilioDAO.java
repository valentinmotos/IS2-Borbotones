package com.example.servidor.dao;

import com.example.servidor.model.Domicilio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomicilioDAO extends JpaRepository<Domicilio, Long> {
}
