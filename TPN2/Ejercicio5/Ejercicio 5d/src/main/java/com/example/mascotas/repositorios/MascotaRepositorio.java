package com.example.mascotas.repositorios;

import com.example.mascotas.entidades.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MascotaRepositorio extends JpaRepository<Mascota, String> {
    List<Mascota> findByUsuarioIdAndBajaIsNull(String usuarioId);
    List<Mascota> findByBajaIsNullAndUsuarioBajaIsNullAndUsuarioIdNot(String usuarioId);
}
