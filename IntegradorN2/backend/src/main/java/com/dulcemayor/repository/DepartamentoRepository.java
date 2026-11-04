package com.dulcemayor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dulcemayor.entity.Departamento;

public interface DepartamentoRepository extends JpaRepository<Departamento, String> {
}
