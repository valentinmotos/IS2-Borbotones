package com.example.cliente.service;

import com.example.cliente.dao.PrestamoDAO;
import com.example.cliente.dto.LibroDTO;
import com.example.cliente.dto.PersonaDTO;
import com.example.cliente.dto.PrestamoDTO;
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

    public List<PrestamoDTO> listar() {
        return prestamoDAO.listar();
    }

    public PrestamoDTO buscar(Long id) {
        return prestamoDAO.buscar(id);
    }

    public PrestamoDTO nuevo() {
        PrestamoDTO prestamo = new PrestamoDTO();
        prestamo.setLibro(new LibroDTO());
        prestamo.setPersona(new PersonaDTO());
        prestamo.setFechaPrestamo(LocalDate.now().toString());
        prestamo.setFechaDevolucion(LocalDate.now().plusDays(7).toString());
        prestamo.setDevuelto(false);
        return prestamo;
    }

    public void guardar(PrestamoDTO prestamo) {
        prestamoDAO.guardar(prestamo);
    }

    public void eliminar(Long id) {
        prestamoDAO.eliminar(id);
    }

    public List<LibroDTO> listarLibros() {
        return libroService.listar();
    }

    public List<PersonaDTO> listarPersonas() {
        return personaService.listar();
    }
}
