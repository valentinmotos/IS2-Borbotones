package com.borbotones.videojuegos.repositories;

import com.borbotones.videojuegos.entities.Videojuego;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideojuegoRepository extends JpaRepository<Videojuego, Long> {

    List<Videojuego> findAllByActivoTrueOrderByTituloAsc();

    Optional<Videojuego> findByIdAndActivoTrue(long id);

    List<Videojuego> findByActivoTrueAndTituloContainingIgnoreCaseOrderByTituloAsc(String titulo);
}
