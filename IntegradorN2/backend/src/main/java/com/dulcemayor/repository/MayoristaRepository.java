package com.dulcemayor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dulcemayor.entity.Mayorista;

public interface MayoristaRepository extends JpaRepository<Mayorista, String> {
}
