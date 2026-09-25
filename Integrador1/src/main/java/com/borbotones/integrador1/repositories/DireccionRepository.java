package com.borbotones.integrador1.repositories;

import com.borbotones.integrador1.entities.Direccion;
import com.borbotones.integrador1.entities.Localidad;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class DireccionRepository {

    @PersistenceContext
    private EntityManager em;

    public Direccion crearDireccion(String calle, String numeracion, String barrio, String manzanaPiso, String casaDepartamento, String referencia, String idLocalidad) {
        Direccion direccion = new Direccion();
        direccion.setCalle(calle);
        direccion.setNumeracion(numeracion);
        direccion.setBarrio(barrio);
        direccion.setManzanaPiso(manzanaPiso);
        direccion.setCasaDepartamento(casaDepartamento);
        direccion.setReferencia(referencia);

        if (idLocalidad != null) {
            Localidad localidad = em.find(Localidad.class, idLocalidad);
            direccion.setLocalidad(localidad);
        }

        em.persist(direccion);
        return direccion;
    }

    public void validar(String calle, String numeracion, String barrio, String manzanaPiso, String casaDepartamento, String referencia, String idLocalidad) {
        if (calle == null || calle.isBlank()) {
            throw new IllegalArgumentException("La calle no puede estar vacía");
        }
        if (numeracion == null || numeracion.isBlank()) {
            throw new IllegalArgumentException("La numeración no puede estar vacía");
        }
    }

    public Direccion buscarDireccionPorCalleNumeracion(String calle, String numeracion) {
        try {
            return em.createQuery(
                            "SELECT d FROM Direccion d WHERE d.calle = :calle AND d.numeracion = :numeracion AND d.eliminado = false",
                            Direccion.class)
                    .setParameter("calle", calle)
                    .setParameter("numeracion", numeracion)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void modificarDireccion(String id, String calle, String numeracion, String barrio, String manzanaPiso, String casaDepartamento, String referencia, String idLocalidad) {
        Direccion direccion = em.find(Direccion.class, id);
        if (direccion != null) {
            direccion.setCalle(calle);
            direccion.setNumeracion(numeracion);
            direccion.setBarrio(barrio);
            direccion.setManzanaPiso(manzanaPiso);
            direccion.setCasaDepartamento(casaDepartamento);
            direccion.setReferencia(referencia);

            if (idLocalidad != null) {
                Localidad localidad = em.find(Localidad.class, idLocalidad);
                direccion.setLocalidad(localidad);
            }

            em.merge(direccion);
        }
    }

    public void eliminarDireccion(String id) {
        Direccion direccion = em.find(Direccion.class, id);
        if (direccion != null) {
            direccion.setEliminado(true);
            em.merge(direccion);
        }
    }
}
