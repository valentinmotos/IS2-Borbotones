package com.example.mascotas.repositorios;
import com.example.mascotas.entidades.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MascotaRepositorio extends JpaRepository<Mascota, String> { }
