
package com.example.mascotas.repositorios;

import com.example.mascotas.dto.ReporteMascotaDTO;
import com.example.mascotas.entidades.Mascota;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MascotaRepositorio extends JpaRepository<Mascota, String> {
    @Query("SELECT c FROM Mascota c WHERE c.usuario.id = :id AND c.baja IS NULL")
    public List<Mascota> buscarMascotasPorUsuario(@Param("id") String id);

    @Query("""
            SELECT new com.example.mascotas.dto.ReporteMascotaDTO(
                m.usuario.nombre,
                m.usuario.apellido,
                m.nombre,
                COUNT(v.id)
            )
            FROM Mascota m
            LEFT JOIN Voto v ON v.mascota2 = m
            WHERE m.baja IS NULL AND m.usuario.baja IS NULL
            GROUP BY m.id, m.usuario.nombre, m.usuario.apellido, m.nombre
            ORDER BY m.usuario.apellido, m.usuario.nombre, m.nombre
            """)
    List<ReporteMascotaDTO> generarReporte();
}
