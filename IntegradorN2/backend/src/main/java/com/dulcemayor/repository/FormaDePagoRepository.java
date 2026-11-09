package com.dulcemayor.repository;

import com.dulcemayor.entity.FormaDePago;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormaDePagoRepository extends JpaRepository<FormaDePago, String> {
    List<FormaDePago> findByEliminadoFalse();
}
