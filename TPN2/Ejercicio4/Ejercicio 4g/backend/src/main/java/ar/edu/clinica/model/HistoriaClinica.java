package ar.edu.clinica.model;
import jakarta.persistence.*;
@Entity public class HistoriaClinica extends Registro {
 @ManyToOne(optional=false) public Paciente paciente;
 @ManyToOne(optional=false) public Usuario usuario;
}
