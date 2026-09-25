package com.zero.ecommerce.services;

import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.repositories.ProvinciaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProvinciaService {

    private final ProvinciaRepository provinciaRepository;
    private final PaisService paisService;

    public ProvinciaService(ProvinciaRepository provinciaRepository, PaisService paisService) {
        this.provinciaRepository = provinciaRepository;
        this.paisService = paisService;
    }

    public Provincia crearProvincia(String nombre, String paisId) {
        Pais pais = paisService.buscarPais(paisId);

        Provincia provincia = new Provincia();
        provincia.setNombre(nombre);
        provincia.setPais(pais);

        return provinciaRepository.save(provincia);
    }

    public Provincia modificarProvincia(String id, String nombre, String paisId) {
        Provincia provincia = buscarProvincia(id);
        Pais pais = paisService.buscarPais(paisId);

        provincia.setNombre(nombre);
        provincia.setPais(pais);

        return provinciaRepository.save(provincia);
    }

    public void eliminarProvincia(String id) {
        Provincia provincia = buscarProvincia(id);
        provinciaRepository.delete(provincia);
    }

    public Provincia buscarProvincia(String id) {
        return provinciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provincia no encontrada"));
    }

    public List<Provincia> listarProvincias() {
        return provinciaRepository.findAll();
    }
}