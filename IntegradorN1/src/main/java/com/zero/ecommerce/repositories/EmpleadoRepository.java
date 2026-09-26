package com.zero.ecommerce.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import com.zero.ecommerce.entities.Empleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, String> {

    Optional<Empleado> findByIdAndEliminadoFalse(String id);

    Optional<Empleado> findByUsuario_IdAndEliminadoFalse(String idUsuario);

    List<Empleado> findByEliminadoFalseOrderByApellidoAscNombreAsc();
}
