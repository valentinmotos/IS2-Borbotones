package com.example.servidor.service;

import com.example.servidor.dao.LibroDAO;
import com.example.servidor.model.Libro;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class LibroService {
    private final LibroDAO libroDAO;
    private final PersonaService personaService;
    private final PdfStorageService pdfStorageService;

    public LibroService(LibroDAO libroDAO, PersonaService personaService, PdfStorageService pdfStorageService) {
        this.libroDAO = libroDAO;
        this.personaService = personaService;
        this.pdfStorageService = pdfStorageService;
    }

    public List<Libro> listar() {
        return libroDAO.findAll();
    }

    public Libro buscar(Long id) {
        return libroDAO.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
    }

    public Libro guardar(Libro libro) {
        return guardar(libro, null);
    }

    public Libro guardar(Libro libro, MultipartFile pdf) {
        String pdfAnterior = libro.getId() == null ? null : buscar(libro.getId()).getPdfNombre();
        // La ruta se obtiene del servidor, nunca del JSON enviado por el cliente.
        libro.setPdfNombre(pdfAnterior);
        if (libro.getPersona() != null && libro.getPersona().getId() != null) {
            libro.setPersona(personaService.buscar(libro.getPersona().getId()));
        }
        String pdfNuevo = null;
        if (pdf != null && !pdf.isEmpty()) {
            if (pdfAnterior != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El libro ya tiene un PDF asociado");
            }
            pdfNuevo = pdfStorageService.guardar(libro.getTitulo(), pdf);
            libro.setPdfNombre(pdfNuevo);
        }
        try {
            return libroDAO.saveAndFlush(libro);
        } catch (RuntimeException ex) {
            if (pdfNuevo != null) {
                pdfStorageService.eliminar(pdfNuevo);
            }
            throw ex;
        }
    }

    public Resource abrirPdf(Long id) {
        return pdfStorageService.abrir(buscar(id).getPdfNombre());
    }

    public void eliminar(Long id) {
        String pdfNombre = buscar(id).getPdfNombre();
        libroDAO.deleteById(id);
        pdfStorageService.eliminar(pdfNombre);
    }
}
