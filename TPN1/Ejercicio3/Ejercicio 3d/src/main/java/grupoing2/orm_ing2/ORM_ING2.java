package grupoing2.orm_ing2;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

public class ORM_ING2 {

    private static final int TOTAL_CLIENTES = 50_000;
    private static final int TOTAL_ARTICULOS = 50_000;
    private static final int TAMANIO_LOTE = 1_000;

    public static void main(String[] args) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("zeroPU");

        cargarDatos(emf);
        medirConsultas(emf);

        emf.close();
    }

    private static void cargarDatos(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        long inicio = System.nanoTime();

        em.getTransaction().begin();

        for (int i = 1; i <= TOTAL_CLIENTES; i++) {
            Cliente cliente = new Cliente(
                    (long) i,
                    30_000_000 + i,
                    "Nombre " + (i % 100),
                    "Apellido " + i
            );

            em.persist(cliente);

            if (i % TAMANIO_LOTE == 0) {
                em.flush();
                em.clear();
            }
        }

        for (int i = 1; i <= TOTAL_ARTICULOS; i++) {
            Articulo articulo = new Articulo(
                    i % 500,
                    "Articulo " + i,
                    100 + (i % 10_000)
            );

            em.persist(articulo);

            if (i % TAMANIO_LOTE == 0) {
                em.flush();
                em.clear();
            }
        }

        em.getTransaction().commit();
        em.close();

        System.out.printf(
                "Carga finalizada: %,d clientes y %,d articulos en %.2f ms%n",
                TOTAL_CLIENTES,
                TOTAL_ARTICULOS,
                aMilisegundos(System.nanoTime() - inicio)
        );
    }

    private static void medirConsultas(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();

        ejecutarConsulta(
                "Cliente por DNI - usa indice",
                () -> em.createQuery(
                                "SELECT c FROM Cliente c WHERE c.dni = :dni",
                                Cliente.class
                        )
                        .setParameter("dni", 30_025_000)
                        .getSingleResult()
        );

        ejecutarConsulta(
                "Cliente por apellido - sin indice",
                () -> em.createQuery(
                                "SELECT c FROM Cliente c WHERE c.apellido = :apellido",
                                Cliente.class
                        )
                        .setParameter("apellido", "Apellido 25000")
                        .getSingleResult()
        );

        ejecutarConsulta(
                "Articulo por denominacion - usa indice",
                () -> em.createQuery(
                                "SELECT a FROM Articulo a WHERE a.denominacion = :denominacion",
                                Articulo.class
                        )
                        .setParameter("denominacion", "Articulo 25000")
                        .getSingleResult()
        );

        ejecutarConsulta(
                "Articulos por precio - sin indice",
                () -> {
                    TypedQuery<Articulo> query = em.createQuery(
                            "SELECT a FROM Articulo a WHERE a.precio = :precio",
                            Articulo.class
                    );

                    query.setParameter("precio", 5_100).getResultList();
                }
        );

        em.close();
    }

    private static void ejecutarConsulta(String descripcion, Runnable consulta) {
        long inicio = System.nanoTime();

        consulta.run();

        long fin = System.nanoTime();

        System.out.printf("%s: %.4f ms%n", descripcion, aMilisegundos(fin - inicio));
    }

    private static double aMilisegundos(long nanosegundos) {
        return nanosegundos / 1_000_000.0;
    }
}