package com.borbotones.videojuegos.repositories;

import com.borbotones.videojuegos.entities.Estudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudioRepository extends JpaRepository<Estudio, Long> {
    List<Estudio> findAllByActivoTrueOrderByNombreAsc();
}
