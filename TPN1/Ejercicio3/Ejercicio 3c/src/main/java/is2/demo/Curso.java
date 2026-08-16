package is2.demo;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cursos")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private int cargaHoraria;

    // N:M Bidireccional - Curso es el lado INVERSO (mappedBy)
    @ManyToMany(mappedBy = "cursos", fetch = FetchType.LAZY)
    private Set<Persona> personas = new HashSet<>();

    public Curso() {}

    public Curso(String nombre, int cargaHoraria) {
        this.nombre = nombre;
        this.cargaHoraria = cargaHoraria;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(int cargaHoraria) { this.cargaHoraria = cargaHoraria; }
    public Set<Persona> getPersonas() { return personas; }

    @Override
    public String toString() {
        return "Curso{id=" + id + ", nombre='" + nombre + "'}";
    }
}