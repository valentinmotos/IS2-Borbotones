package is2.demo;

import jakarta.persistence.*;

@Entity
@Table(name = "telefonos")
public class Telefono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;

    // N:1 - Telefono es SIEMPRE el lado propietario en un OneToMany bidireccional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    public Telefono() {}

    public Telefono(String numero) {
        this.numero = numero;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    @Override
    public String toString() {
        return "Telefono{id=" + id + ", numero='" + numero + "'}";
    }
}