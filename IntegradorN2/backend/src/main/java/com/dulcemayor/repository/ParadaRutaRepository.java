package com.dulcemayor.repository;

import com.dulcemayor.entity.ParadaRuta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParadaRutaRepository extends JpaRepository<ParadaRuta, String> {
    List<ParadaRuta> findByRuta_IdAndEliminadoFalseOrderByOrdenAsc(String rutaId);
}
