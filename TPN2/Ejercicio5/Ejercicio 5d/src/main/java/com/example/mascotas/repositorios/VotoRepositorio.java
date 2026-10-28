package com.example.mascotas.repositorios;

import com.example.mascotas.entidades.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface VotoRepositorio extends JpaRepository<Voto, String> {
    Optional<Voto> findByMascota1IdAndMascota2Id(String origen, String destino);
    @Query("select v from Voto v where v.mascota1.usuario.id = :usuario or v.mascota2.usuario.id = :usuario order by v.fecha desc")
    List<Voto> delUsuario(@Param("usuario") String usuario);
}
