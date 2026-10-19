package com.example.servidor.service;

import com.example.servidor.dao.PrestamoDAO;
import com.example.servidor.model.Libro;
import com.example.servidor.model.Persona;
import com.example.servidor.model.Prestamo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrestamoService extends BaseService<Prestamo> {
    private final PrestamoDAO prestamoDAO;
    private final LibroService libroService;
    private final PersonaService personaService;

    public PrestamoService(PrestamoDAO prestamoDAO, LibroService libroService, PersonaService personaService) {
        super(prestamoDAO, "Prestamo no encontrado");
        this.prestamoDAO = prestamoDAO;
        this.libroService = libroService;
        this.personaService = personaService;
    }

    public List<Prestamo> listarConDevolucionManana() {
        return prestamoDAO.findByFechaDevolucionAndDevueltoFalse(LocalDate.now().plusDays(1));
    }

    @Override
    protected void completarRelaciones(Prestamo prestamo) {
        Libro libro = prestamo.getLibro();
        Persona persona = prestamo.getPersona();
        prestamo.setLibro(libro != null && libro.getId() != null ? libroService.buscar(libro.getId()) : null);
        prestamo.setPersona(persona != null && persona.getId() != null ? personaService.buscar(persona.getId()) : null);
    }

    @Override
    protected void asignarValoresPorDefecto(Prestamo prestamo) {
        if (prestamo.getDevuelto() == null) {
            prestamo.setDevuelto(false);
        }
    }
}
