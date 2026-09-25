package com.borbotones.integrador1.repositories;

import com.borbotones.integrador1.entities.Imagen;
import com.borbotones.integrador1.entities.Persona;
import com.borbotones.integrador1.entities.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class PersonaRepository {

    @PersistenceContext
    private EntityManager em;

    public Persona buscarPersona(String id) {
        return em.find(Persona.class, id);
    }

    public void eliminarPersona(String id) {
        Persona persona = buscarPersona(id);
        if (persona != null) {
            persona.setEliminado(true);
            em.merge(persona);
        }
    }

    public Persona asociarImagenPersona(String id, String idImagen) {
        Persona persona = buscarPersona(id);
        if (persona != null) {
            Imagen imagen = em.find(Imagen.class, idImagen);
            persona.setImagen(imagen);
            return em.merge(persona);
        }
        return null;
    }

    public Persona asociarUsuarioPersona(String id, String idUsuario) {
        Persona persona = buscarPersona(id);
        if (persona != null) {
            Usuario usuario = em.find(Usuario.class, idUsuario);
            persona.setUsuario(usuario);
            return em.merge(persona);
        }
        return null;
    }
}