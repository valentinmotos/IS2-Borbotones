package com.zero.ecommerce.services;

import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.repositories.DepartamentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;
    private final ProvinciaService provinciaService;

    public DepartamentoService(DepartamentoRepository departamentoRepository, ProvinciaService provinciaService) {
        this.departamentoRepository = departamentoRepository;
        this.provinciaService = provinciaService;
    }

    public Departamento crearDepartamento(String nombre, String provinciaId) {
        Provincia provincia = provinciaService.buscarProvincia(provinciaId);

        Departamento departamento = new Departamento();
        departamento.setNombre(nombre);
        departamento.setProvincia(provincia);

        return departamentoRepository.save(departamento);
    }

    public Departamento modificarDepartamento(String id, String nombre, String provinciaId) {
        Departamento departamento = buscarDepartamento(id);
        Provincia provincia = provinciaService.buscarProvincia(provinciaId);

        departamento.setNombre(nombre);
        departamento.setProvincia(provincia);

        return departamentoRepository.save(departamento);
    }

    public void eliminarDepartamento(String id) {
        Departamento departamento = buscarDepartamento(id);
        departamentoRepository.delete(departamento);
    }

    public Departamento buscarDepartamento(String id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));
    }

    public List<Departamento> listarDepartamentos() {
        return departamentoRepository.findAll();
    }
}