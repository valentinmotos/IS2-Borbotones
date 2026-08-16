package services_DAO;


import grupoing2.orm_ing2.Factura;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;

public class FacturaDAO {

    private final EntityManager entityManager;
    private final EntityTransaction transaction;

    public FacturaDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.transaction = entityManager.getTransaction();
    }
    
    public void crearFactura( Factura factura ) {
        this.transaction.begin();
        entityManager.persist(factura);
        this.transaction.commit();
    }
    
    public Factura buscarFactura(Long id) {
        return entityManager.find(Factura.class, id);
    }

    public void modificarFactura(Factura factura) {
        this.transaction.begin();
        entityManager.merge(factura);
        this.transaction.commit();
    }

    public void eliminarFactura(Long id) {
        Factura factura = entityManager.find(Factura.class, id);
        this.transaction.begin();
        entityManager.remove(factura);
        this.transaction.commit();
    }

    public List<Factura> listarFacturas() {
        return entityManager
                .createQuery("SELECT * FROM Factura", Factura.class)
                .getResultList();
    }
}