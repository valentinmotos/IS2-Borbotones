package grupoing2.orm_ing2;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class ORM_ING2 {

    public static void main(String[] args) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("zeroPU");
        
        EntityManager em = emf.createEntityManager();
        
        List<Categoria> categorias = em.createQuery("FROM Categoria", Categoria.class).getResultList();
        
        for (int i = 1; i <= 10; i++) {
            
            Categoria categoria = new Categoria();
            categoria.setDenominacion( "Verduleria "  );
            
            em.persist( categoria );
        }
        
        
        em.close();
        
        emf.close();
        
        System.out.println("JPA funciona correctamente");
        
        emf.close();
    }
}