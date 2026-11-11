package com.dulcemayor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dulcemayor.entity.Direccion;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.DireccionRepository;

/** E2-06 y E2-07: alta y modificación de direcciones con su ubicación en el mapa (componente de E1-04). */
@Service
@Transactional(readOnly = true)
public class DireccionService {

    private final DireccionRepository direccionRepository;
    private final LocalidadService localidadService;

    public DireccionService(DireccionRepository direccionRepository, LocalidadService localidadService) {
        this.direccionRepository = direccionRepository;
        this.localidadService = localidadService;
    }

    @Transactional
    public Direccion crearDireccion(String calle, String numeracion, String barrio, String manzanaPiso,
            String casaDepartamento, String referencia, Double latitud, Double longitud, String idLocalidad) {
        return guardar(new Direccion(), calle, numeracion, barrio, manzanaPiso, casaDepartamento, referencia,
                latitud, longitud, idLocalidad);
    }

    /** E2-07: actualiza una dirección existente con las mismas validaciones del alta. */
    @Transactional
    public Direccion modificarDireccion(Direccion direccion, String calle, String numeracion, String barrio,
            String manzanaPiso, String casaDepartamento, String referencia, Double latitud, Double longitud,
            String idLocalidad) {
        return guardar(direccion, calle, numeracion, barrio, manzanaPiso, casaDepartamento, referencia,
                latitud, longitud, idLocalidad);
    }

    private Direccion guardar(Direccion direccion, String calle, String numeracion, String barrio,
            String manzanaPiso, String casaDepartamento, String referencia, Double latitud, Double longitud,
            String idLocalidad) {
        validar(calle, numeracion, latitud, longitud);
        direccion.setLocalidad(localidadService.buscarEntidad(idLocalidad));
        direccion.setCalle(calle.strip());
        direccion.setNumeracion(numeracion.strip());
        direccion.setBarrio(limpiar(barrio));
        direccion.setManzanaPiso(limpiar(manzanaPiso));
        direccion.setCasaDepartamento(limpiar(casaDepartamento));
        direccion.setReferencia(limpiar(referencia));
        direccion.setLatitud(latitud);
        direccion.setLongitud(longitud);
        return direccionRepository.save(direccion);
    }

    void validar(String calle, String numeracion, Double latitud, Double longitud) {
        if (calle == null || calle.isBlank()) {
            throw new ErrorServiceException("Ingresá la calle.", "calle");
        }
        if (numeracion == null || numeracion.isBlank()) {
            throw new ErrorServiceException("Ingresá la numeración.", "numeracion");
        }
        if (latitud == null || longitud == null || !Double.isFinite(latitud) || !Double.isFinite(longitud)) {
            throw new ErrorServiceException("Marcá la ubicación en el mapa.", "latitud");
        }
        if (Math.abs(latitud) > 90 || Math.abs(longitud) > 180) {
            throw new ErrorServiceException("La ubicación marcada no es válida.", "latitud");
        }
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
