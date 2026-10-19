package ar.edu.clinica.model;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity public class FotoPaciente extends Registro {
 public String nombre; public String mime;
 @Lob @JsonIgnore public byte[] contenido;
}
