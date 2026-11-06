package com.dulcemayor.repository;

import com.dulcemayor.entity.Empleado;
import com.dulcemayor.entity.enums.RolUsuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, String> {

    List<Empleado> findByEliminadoFalse();

    List<Empleado> findByUsuario_RolAndEliminadoFalse(RolUsuario rol);
}
