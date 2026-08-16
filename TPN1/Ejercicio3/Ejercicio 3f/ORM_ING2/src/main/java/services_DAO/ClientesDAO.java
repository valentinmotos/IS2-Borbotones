package services_DAO;


import grupoing2.orm_ing2.Cliente;
import grupoing2.orm_ing2.EstadoFactura;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;

public class ClientesDAO {

    private final EntityManager entityManager;
    private final EntityTransaction transaction;

    public ClientesDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.transaction = entityManager.getTransaction();
    }
    
    public void crearCliente( Cliente cliente ) {
        this.transaction.begin();
        entityManager.persist(cliente);
        this.transaction.commit();
    }
    
    public Cliente buscarCliente(Long id) {
        return entityManager.find(Cliente.class, id);
    }

    public void modificarCliente(Cliente cliente) {
        this.transaction.begin();
        entityManager.merge(cliente);
        this.transaction.commit();
    }

    public void eliminarCliente(Long id) {
        Cliente cliente = entityManager.find(Cliente.class, id);

        if (cliente != null) {
            this.transaction.begin();
            entityManager.remove(cliente);
            this.transaction.commit();
        }
    }

    public List<Cliente> listarClientes() {
        return entityManager
                .createQuery("SELECT c FROM Cliente c", Cliente.class)
                .getResultList();
    }
}