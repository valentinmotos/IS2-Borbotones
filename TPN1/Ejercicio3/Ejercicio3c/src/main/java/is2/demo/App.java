package is2.demo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

public class App {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("demoPU");
        EntityManager em = emf.createEntityManager();

        // ---------- Carga de datos ----------
        em.getTransaction().begin();

        Persona valentin = new Persona("Valentín", "valentin@example.com");
        Persona ana = new Persona("Ana", "ana@example.com");
        Persona luis = new Persona("Luis", "luis@example.com");

        valentin.setDomicilio(new Domicilio("San Martín 123", "Mendoza"));
        ana.setDomicilio(new Domicilio("Belgrano 456", "Luján de Cuyo"));
        // Luis queda sin domicilio a propósito, para probar la consulta 3

        valentin.addTelefono(new Telefono("261-1111111"));
        valentin.addTelefono(new Telefono("261-2222222"));
        ana.addTelefono(new Telefono("261-3333333"));

        Curso bd = new Curso("Bases de Datos", 96);
        Curso paradigmas = new Curso("Paradigmas de Programación", 128);

        valentin.addCurso(bd);
        valentin.addCurso(paradigmas);
        ana.addCurso(bd);

        em.persist(valentin);
        em.persist(ana);
        em.persist(luis);

        em.getTransaction().commit();

        System.out.println("\n===== DATOS CARGADOS =====");

        // ---------- Consultas JPQL ----------

        //1. Personas con más de un teléfono
        TypedQuery<Persona> q1 = em.createQuery(
            "SELECT p FROM Persona p WHERE SIZE(p.telefonos) > 1", Persona.class);
        q1.getResultList().forEach(System.out::println);

        //2.  Cursos con cantidad de inscriptos
        TypedQuery<Object[]> q2 = em.createQuery(
            "SELECT c.nombre, COUNT(p) FROM Curso c JOIN c.personas p GROUP BY c.nombre",
            Object[].class);
        for (Object[] fila : q2.getResultList()) {
            System.out.println(fila[0] + " -> " + fila[1] + " inscriptos");
        }

        //3. Personas sin domicilio asignado
        TypedQuery<Persona> q3 = em.createQuery(
            "SELECT p FROM Persona p LEFT JOIN p.domicilio d WHERE d IS NULL", Persona.class);
        q3.getResultList().forEach(System.out::println);

        //4. Personas inscritas en 'Bases de Datos'
        TypedQuery<Persona> q4 = em.createQuery(
            "SELECT p FROM Persona p JOIN p.cursos c WHERE c.nombre = :nombreCurso", Persona.class);
        q4.setParameter("nombreCurso", "Bases de Datos");
        q4.getResultList().forEach(System.out::println);

        //5. Cursos con carga horaria mayor al promedio
        TypedQuery<Curso> q5 = em.createQuery(
            "SELECT c FROM Curso c WHERE c.cargaHoraria > " +
            "(SELECT AVG(c2.cargaHoraria) FROM Curso c2)", Curso.class);
        q5.getResultList().forEach(System.out::println);

        //6. JOIN FETCH: persona + teléfonos en una sola consulta
        TypedQuery<Persona> q6 = em.createQuery(
            "SELECT DISTINCT p FROM Persona p JOIN FETCH p.telefonos", Persona.class);
        for (Persona p : q6.getResultList()) {
            System.out.println(p + " - Teléfonos: " + p.getTelefonos());
        }

        em.close();
        emf.close();
    }
}