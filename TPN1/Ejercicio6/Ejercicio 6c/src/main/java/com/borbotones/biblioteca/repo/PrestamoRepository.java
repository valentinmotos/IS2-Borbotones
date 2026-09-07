package com.borbotones.biblioteca.repo;
import com.borbotones.biblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findByAltaTrueOrderByFechaPrestamoDesc();
}