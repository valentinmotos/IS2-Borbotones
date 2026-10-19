package ar.edu.clinica.model;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity public class Usuario extends Registro {
 @Column(nullable=false,unique=true,length=80) public String nombreUsuario;
 @JsonIgnore @Column(nullable=false) public String clave;
}
