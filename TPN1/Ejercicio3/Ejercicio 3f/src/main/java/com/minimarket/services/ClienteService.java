package com.minimarket.services;

import com.minimarket.entities.Cliente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class ClienteService {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("minimarketPU");

    public void crearCliente(String nombre, String apellido, String dni, String email) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Cliente cliente = new Cliente(nombre, apellido, dni, email);
            em.persist(cliente);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Cliente buscarCliente(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Cliente.class, id);
        } finally {
            em.close();
        }
    }

    public void modificarCliente(String id, String nombre, String apellido, String dni, String email) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Cliente cliente = em.find(Cliente.class, id);
            if (cliente == null) {
                throw new RuntimeException("Cliente no encontrado con ID: " + id);
            }
            cliente.setNombre(nombre);
            cliente.setApellido(apellido);
            cliente.setDni(dni);
            cliente.setEmail(email);
            em.merge(cliente);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void eliminarCliente(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Cliente cliente = em.find(Cliente.class, id);
            if (cliente == null) {
                throw new RuntimeException("Cliente no encontrado con ID: " + id);
            }
            cliente.setEliminado(true);
            em.merge(cliente);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Cliente> listarClientes() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Cliente c", Cliente.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Cliente> listarClientesActivos() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Cliente c WHERE c.eliminado = false", Cliente.class).getResultList();
        } finally {
            em.close();
        }
    }
}
