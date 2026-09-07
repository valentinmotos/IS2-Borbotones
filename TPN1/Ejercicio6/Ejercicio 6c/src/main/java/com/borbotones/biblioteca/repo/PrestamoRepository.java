package com.borbotones.biblioteca.repo;
import com.borbotones.biblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import java.util.List;
public interface PrestamoRepository extends JpaRepository<Prestamo, Long>, RevisionRepository<Prestamo, Long, Integer> {
    List<Prestamo> findByAltaTrueOrderByFechaPrestamoDesc();
}
