package is2.demo;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "personas")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String email;

    // 1:1 Bidireccional - Persona es el lado INVERSO (mappedBy)
    @OneToOne(mappedBy = "persona", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Domicilio domicilio;

    // 1:N Bidireccional - Persona es el lado INVERSO (mappedBy)
    @OneToMany(mappedBy = "persona", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Telefono> telefonos = new ArrayList<>();

    // N:M Bidireccional - Persona es el lado PROPIETARIO (dueña de la tabla intermedia)
    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinTable(
        name = "persona_curso",
        joinColumns = @JoinColumn(name = "persona_id"),
        inverseJoinColumns = @JoinColumn(name = "curso_id")
    )
    private Set<Curso> cursos = new HashSet<>();

    public Persona() {}

    public Persona(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
    }

    public void setDomicilio(Domicilio domicilio) {
        this.domicilio = domicilio;
        if (domicilio != null) domicilio.setPersona(this);
    }

    public void addTelefono(Telefono telefono) {
        telefonos.add(telefono);
        telefono.setPersona(this);
    }

    public void removeTelefono(Telefono telefono) {
        telefonos.remove(telefono);
        telefono.setPersona(null);
    }

    public void addCurso(Curso curso) {
        cursos.add(curso);
        curso.getPersonas().add(this);
    }

    public void removeCurso(Curso curso) {
        cursos.remove(curso);
        curso.getPersonas().remove(this);
    }

    // Getters y setters
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Domicilio getDomicilio() { return domicilio; }
    public List<Telefono> getTelefonos() { return telefonos; }
    public Set<Curso> getCursos() { return cursos; }

    @Override
    public String toString() {
        return "Persona{id=" + id + ", nombre='" + nombre + "', email='" + email + "'}";
    }
}