package services_DAO;


import grupoing2.orm_ing2.EstadoFactura;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;

public class EstadoFacturaDAO {

    private final EntityManager entityManager;
    private final EntityTransaction transaction;
    
    public EstadoFacturaDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.transaction = entityManager.getTransaction();
    }
    
    public void crearFactura( EstadoFactura estadoFactura ) {
        this.transaction.begin();
        entityManager.persist(estadoFactura);
        this.transaction.commit();
    }

    public void modificarEstadoFactura( EstadoFactura estadoFactura ) {
        this.transaction.begin();
        entityManager.merge(estadoFactura);
        this.transaction.commit();
    }

    public void eliminarEstadoFactura( EstadoFactura estadoFactura ) {
        this.transaction.begin();
        entityManager.remove(estadoFactura);
        this.transaction.commit();
    }

    public List<EstadoFactura> listarEstadosFacturas() {
        return entityManager
                .createQuery("SELECT c FROM EstadoFactura c", EstadoFactura.class)
                .getResultList();
    }
}