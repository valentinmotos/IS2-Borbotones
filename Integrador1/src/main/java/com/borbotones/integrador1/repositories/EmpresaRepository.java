package com.borbotones.integrador1.repositories;

import com.borbotones.integrador1.entities.Contacto;
import com.borbotones.integrador1.entities.Empresa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class EmpresaRepository {

    @PersistenceContext
    private EntityManager em;

    public void crearEmpresa(String razonSocial, String cuit, Contacto contacto) {
        Empresa empresa = new Empresa();
        empresa.setRazonSocial(razonSocial);
        empresa.setCuit(cuit);
        if (contacto != null) {
            List<Contacto> listaContactos = new ArrayList<>();
            listaContactos.add(contacto);
            empresa.setContactos(listaContactos);
        }
        em.persist(empresa);
    }

    public void validar(String razonSocial, String cuit, Contacto contacto) {
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new IllegalArgumentException("La razón social no puede estar vacía");
        }
        if (cuit == null || cuit.isBlank()) {
            throw new IllegalArgumentException("El CUIT no puede estar vacío");
        }
    }

    public Empresa buscarEmpresa(String id) {
        return em.find(Empresa.class, id);
    }

    public Empresa buscarEmpresaPorNombre(String nombre) {
        try {
            return em.createQuery(
                            "SELECT e FROM Empresa e WHERE e.razonSocial = :nombre AND e.eliminado = false",
                            Empresa.class)
                    .setParameter("nombre", nombre)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void modificarEmpresa(String id, String razonSocial, String cuit, Contacto contacto) {
        Empresa empresa = buscarEmpresa(id);
        if (empresa != null) {
            empresa.setRazonSocial(razonSocial);
            empresa.setCuit(cuit);
            if (contacto != null) {
                if (empresa.getContactos() == null) {
                    empresa.setContactos(new ArrayList<>());
                }
                if (!empresa.getContactos().contains(contacto)) {
                    empresa.getContactos().add(contacto);
                }
            }
            em.merge(empresa);
        }
    }

    public void eliminarEmpresa(String id) {
        Empresa empresa = buscarEmpresa(id);
        if (empresa != null) {
            empresa.setEliminado(true);
            em.merge(empresa);
        }
    }

    public Collection<Empresa> listarEmpresa() {
        return em.createQuery("SELECT e FROM Empresa e", Empresa.class)
                .getResultList();
    }

    public Collection<Empresa> listarEmpresaActiva() {
        return em.createQuery("SELECT e FROM Empresa e WHERE e.eliminado = false", Empresa.class)
                .getResultList();
    }
}