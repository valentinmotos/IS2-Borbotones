package com.minimarket.services;

import com.minimarket.entities.Cliente;
import com.minimarket.entities.Factura;
import com.minimarket.enums.EstadoFactura;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Date;
import java.util.List;

public class FacturaService {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("minimarketPU");

    public void crearFactura(long numeroFactura, Date fechaFactura, double totalPagado, EstadoFactura estado, String clienteId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Cliente cliente = em.find(Cliente.class, clienteId);
            if (cliente == null) {
                throw new RuntimeException("Cliente no encontrado con ID: " + clienteId);
            }
            Factura factura = new Factura(numeroFactura, fechaFactura, totalPagado, estado, cliente);
            em.persist(factura);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Factura buscarFactura(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Factura.class, id);
        } finally {
            em.close();
        }
    }

    public void modificarFactura(String id, long numeroFactura, Date fechaFactura, double totalPagado, EstadoFactura estado, String clienteId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Factura factura = em.find(Factura.class, id);
            if (factura == null) {
                throw new RuntimeException("Factura no encontrada con ID: " + id);
            }
            Cliente cliente = em.find(Cliente.class, clienteId);
            if (cliente == null) {
                throw new RuntimeException("Cliente no encontrado con ID: " + clienteId);
            }
            factura.setNumeroFactura(numeroFactura);
            factura.setFechaFactura(fechaFactura);
            factura.setTotalPagado(totalPagado);
            factura.setEstado(estado);
            factura.setCliente(cliente);
            em.merge(factura);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void eliminarFactura(String id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Factura factura = em.find(Factura.class, id);
            if (factura == null) {
                throw new RuntimeException("Factura no encontrada con ID: " + id);
            }
            factura.setEliminado(true);
            em.merge(factura);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Factura> listarFacturas() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT f FROM Factura f", Factura.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Factura> listarFacturasActivas() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT f FROM Factura f WHERE f.eliminado = false", Factura.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Factura> listarFacturasPorEstado(EstadoFactura estado) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT f FROM Factura f WHERE f.estado = :estado", Factura.class)
                    .setParameter("estado", estado)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
