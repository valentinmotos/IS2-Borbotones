package com.zero.ecommerce.services;

import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.repositories.PaisRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaisService {

    private final PaisRepository paisRepository;

    public PaisService(PaisRepository paisRepository) {
        this.paisRepository = paisRepository;
    }

    public Pais crearPais(String nombre) {
        Pais pais = new Pais();
        pais.setNombre(nombre);
        return paisRepository.save(pais);
    }

    public Pais modificarPais(String id, String nombre) {
        Pais pais = buscarPais(id);
        pais.setNombre(nombre);
        return paisRepository.save(pais);
    }

    public void eliminarPais(String id) {
        Pais pais = buscarPais(id);
        paisRepository.delete(pais);
    }

    public Pais buscarPais(String id) {
        return paisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("País no encontrado"));
    }

    public List<Pais> listarPaises() {
        return paisRepository.findAll();
    }
}
