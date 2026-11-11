package com.dulcemayor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dulcemayor.entity.Direccion;
import com.dulcemayor.entity.Localidad;
import com.dulcemayor.repository.DireccionRepository;

@Service
@Transactional(readOnly = true)
public class DireccionService {
    private final DireccionRepository direccionRepository;
    private final LocalidadService localidadService;
    public DireccionService(DireccionRepository direccionRepository, LocalidadService localidadService) {
        this.direccionRepository = direccionRepository; this.localidadService = localidadService;
    }
    @Transactional
    public Direccion crear(String localidadId, String calle, String numero, String barrio, String manzanaPiso,
            String casaDepartamento, String referencia, double latitud, double longitud) {
        Localidad localidad = localidadService.buscarEntidad(localidadId);
        Direccion d = new Direccion(); d.setLocalidad(localidad); d.setCalle(calle.strip()); d.setNumeracion(numero.strip());
        d.setBarrio(limpiar(barrio)); d.setManzanaPiso(limpiar(manzanaPiso)); d.setCasaDepartamento(limpiar(casaDepartamento));
        d.setReferencia(limpiar(referencia)); d.setLatitud(latitud); d.setLongitud(longitud);
        return direccionRepository.save(d);
    }
    private String limpiar(String v) { return v == null || v.isBlank() ? null : v.strip(); }
}
