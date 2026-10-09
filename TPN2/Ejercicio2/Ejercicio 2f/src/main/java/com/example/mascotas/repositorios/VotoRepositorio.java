package com.example.mascotas.repositorios;
import com.example.mascotas.entidades.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VotoRepositorio extends JpaRepository<Voto, String> {
    boolean existsByMascota1IdAndMascota2Id(String mascota1Id, String mascota2Id);
}
