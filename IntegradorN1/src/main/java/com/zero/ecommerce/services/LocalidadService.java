package com.zero.ecommerce.services;

import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.repositories.LocalidadRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalidadService {

    private final LocalidadRepository localidadRepository;
    private final DepartamentoService departamentoService;

    public LocalidadService(LocalidadRepository localidadRepository, DepartamentoService departamentoService) {
        this.localidadRepository = localidadRepository;
        this.departamentoService = departamentoService;
    }

    public Localidad crearLocalidad(String nombre, String codigoPostal, String departamentoId) {
        Departamento departamento = departamentoService.buscarDepartamento(departamentoId);

        Localidad localidad = new Localidad();
        localidad.setNombre(nombre);
        localidad.setCodigoPostal(codigoPostal);
        localidad.setDepartamento(departamento);

        return localidadRepository.save(localidad);
    }

    public Localidad modificarLocalidad(String id, String nombre, String codigoPostal, String departamentoId) {
        Localidad localidad = buscarLocalidad(id);
        Departamento departamento = departamentoService.buscarDepartamento(departamentoId);

        localidad.setNombre(nombre);
        localidad.setCodigoPostal(codigoPostal);
        localidad.setDepartamento(departamento);

        return localidadRepository.save(localidad);
    }

    public void eliminarLocalidad(String id) {
        Localidad localidad = buscarLocalidad(id);
        localidadRepository.delete(localidad);
    }

    public Localidad buscarLocalidad(String id) {
        return localidadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Localidad no encontrada"));
    }

    public List<Localidad> listarLocalidades() {
        return localidadRepository.findAll();
    }
}