package com.example.cliente.service;

import com.example.cliente.dao.PrestamoDAO;
import com.example.cliente.dto.LibroDTO;
import com.example.cliente.dto.PersonaDTO;
import com.example.cliente.dto.PrestamoDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrestamoService extends BaseService<PrestamoDTO> {
    private final LibroService libroService;
    private final PersonaService personaService;

    public PrestamoService(PrestamoDAO prestamoDAO, LibroService libroService, PersonaService personaService) {
        super(prestamoDAO);
        this.libroService = libroService;
        this.personaService = personaService;
    }

    public List<LibroDTO> listarLibros() {
        return libroService.listar();
    }

    public List<PersonaDTO> listarPersonas() {
        return personaService.listar();
    }

    @Override
    protected PrestamoDTO crearDTO() {
        return new PrestamoDTO();
    }

    @Override
    protected void inicializarRelaciones(PrestamoDTO prestamo) {
        prestamo.setLibro(new LibroDTO());
        prestamo.setPersona(new PersonaDTO());
    }

    @Override
    protected void asignarValoresPorDefecto(PrestamoDTO prestamo) {
        prestamo.setFechaPrestamo(LocalDate.now().toString());
        prestamo.setFechaDevolucion(LocalDate.now().plusDays(7).toString());
        prestamo.setDevuelto(false);
    }
}
