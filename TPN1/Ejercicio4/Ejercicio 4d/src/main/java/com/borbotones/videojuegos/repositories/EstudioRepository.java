package com.borbotones.videojuegos.repositories;

import com.borbotones.videojuegos.entities.Estudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstudioRepository extends JpaRepository<Estudio, Long>, RevisionRepository<Estudio, Long, Integer> {
}
