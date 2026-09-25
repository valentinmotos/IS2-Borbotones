package com.zero.ecommerce.services;

import com.zero.ecommerce.entities.Departamento;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.entities.Pais;
import com.zero.ecommerce.entities.Provincia;
import com.zero.ecommerce.repositories.DepartamentoRepository;
import com.zero.ecommerce.repositories.LocalidadRepository;
import com.zero.ecommerce.repositories.PaisRepository;
import com.zero.ecommerce.repositories.ProvinciaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UbicacionService {

    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final DepartamentoRepository departamentoRepository;
    private final LocalidadRepository localidadRepository;


    public UbicacionService(
            PaisRepository paisRepository,
            ProvinciaRepository provinciaRepository,
            DepartamentoRepository departamentoRepository,
            LocalidadRepository localidadRepository) {

        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.departamentoRepository = departamentoRepository;
        this.localidadRepository = localidadRepository;
    }

    public Pais crearPais(String nombre) {

        Pais pais = new Pais();
        pais.setNombre(nombre);

        return paisRepository.save(pais);
    }


    public Pais modificarPais(String id, String nombre) {

        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("País no encontrado"));

        pais.setNombre(nombre);

        return paisRepository.save(pais);
    }


    public void eliminarPais(String id) {

        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("País no encontrado"));

        paisRepository.delete(pais);
    }


    public List<Pais> listarPaises() {
        return paisRepository.findAll();
    }

    public Provincia crearProvincia(String nombre, String paisId) {

        Pais pais = paisRepository.findById(paisId).orElseThrow( () -> new RuntimeException("País no encontrado"));

        Provincia provincia = new Provincia();
        provincia.setNombre(nombre);
        provincia.setPais(pais);

        return provinciaRepository.save(provincia);
    }


    public Provincia modificarProvincia(
            String id,
            String nombre,
            String paisId) {

        Provincia provincia = provinciaRepository.findById(id).orElseThrow(() -> new RuntimeException("Provincia no encontrada"));

        Pais pais = paisRepository.findById(paisId).orElseThrow(() -> new RuntimeException("País no encontrado"));

        provincia.setNombre(nombre);
        provincia.setPais(pais);

        return provinciaRepository.save(provincia);
    }


    public void eliminarProvincia(String id) {

        Provincia provincia = provinciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provincia no encontrada"));

        provinciaRepository.delete(provincia);
    }


    public List<Provincia> listarProvincias() {
        return provinciaRepository.findAll();
    }

    public Departamento crearDepartamento(
            String nombre,
            String provinciaId) {

        Provincia provincia = provinciaRepository.findById(provinciaId)
                .orElseThrow(() -> new RuntimeException("Provincia no encontrada"));

        Departamento departamento = new Departamento();
        departamento.setNombre(nombre);
        departamento.setProvincia(provincia);

        return departamentoRepository.save(departamento);
    }

    public Departamento modificarDepartamento(
            String id,
            String nombre,
            String provinciaId) {

        Departamento departamento = departamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        Provincia provincia = provinciaRepository.findById(provinciaId)
                .orElseThrow(() -> new RuntimeException("Provincia no encontrada"));

        departamento.setNombre(nombre);
        departamento.setProvincia(provincia);

        return departamentoRepository.save(departamento);
    }


    public void eliminarDepartamento(String id) {

        Departamento departamento = departamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        departamentoRepository.delete(departamento);
    }


    public List<Departamento> listarDepartamentos() {
        return departamentoRepository.findAll();
    }


    // =========================================================
    // LOCALIDAD
    // =========================================================

    public Localidad crearLocalidad(
            String nombre,
            String codigoPostal,
            String departamentoId) {

        Departamento departamento = departamentoRepository.findById(departamentoId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        Localidad localidad = new Localidad();
        localidad.setNombre(nombre);
        localidad.setCodigoPostal(codigoPostal);
        localidad.setDepartamento(departamento);

        return localidadRepository.save(localidad);
    }


    public Localidad modificarLocalidad(
            String id,
            String nombre,
            String codigoPostal,
            String departamentoId) {

        Localidad localidad = localidadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Localidad no encontrada"));

        Departamento departamento = departamentoRepository.findById(departamentoId)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado"));

        localidad.setNombre(nombre);
        localidad.setCodigoPostal(codigoPostal);
        localidad.setDepartamento(departamento);

        return localidadRepository.save(localidad);
    }


    public void eliminarLocalidad(String id) {

        Localidad localidad = localidadRepository.findById(id).orElseThrow(() -> new RuntimeException("Localidad no encontrada"));

        localidadRepository.delete(localidad);
    }

    public Localidad buscarLocalidad(String id) {

        return localidadRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Localidad no encontrada")
                );
    }

    public List<Localidad> listarLocalidades() {
        return localidadRepository.findAll();
    }
}
