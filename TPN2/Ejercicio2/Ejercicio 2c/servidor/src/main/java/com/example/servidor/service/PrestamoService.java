package com.example.servidor.service;

import com.example.servidor.dao.PrestamoDAO;
import com.example.servidor.model.Prestamo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrestamoService {
    private final PrestamoDAO prestamoDAO;
    private final LibroService libroService;
    private final PersonaService personaService;

    public PrestamoService(PrestamoDAO prestamoDAO, LibroService libroService, PersonaService personaService) {
        this.prestamoDAO = prestamoDAO;
        this.libroService = libroService;
        this.personaService = personaService;
    }

    public List<Prestamo> listar() {
        return prestamoDAO.findAll();
    }

    public Prestamo buscar(Long id) {
        return prestamoDAO.findById(id).orElseThrow(() -> new IllegalArgumentException("Prestamo no encontrado"));
    }

    public Prestamo guardar(Prestamo prestamo) {
        if (prestamo.getLibro() != null && prestamo.getLibro().getId() != null) {
            prestamo.setLibro(libroService.buscar(prestamo.getLibro().getId()));
        }
        if (prestamo.getPersona() != null && prestamo.getPersona().getId() != null) {
            prestamo.setPersona(personaService.buscar(prestamo.getPersona().getId()));
        }
        if (prestamo.getDevuelto() == null) {
            prestamo.setDevuelto(false);
        }
        return prestamoDAO.save(prestamo);
    }

    public void eliminar(Long id) {
        prestamoDAO.deleteById(id);
    }

    public List<Prestamo> listarConDevolucionManana() {
        return prestamoDAO.findByFechaDevolucionAndDevueltoFalse(LocalDate.now().plusDays(1));
    }
}
