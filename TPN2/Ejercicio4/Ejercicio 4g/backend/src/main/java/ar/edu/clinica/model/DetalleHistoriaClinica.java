package ar.edu.clinica.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity public class DetalleHistoriaClinica extends Registro {
 public LocalDate fechaHistoria;
 @Column(nullable=false,length=10000) public String detalleHistoria;
 @ManyToOne(optional=false) public Medico medico;
 @ManyToOne(optional=false) @JsonIgnore public HistoriaClinica historia;
}
