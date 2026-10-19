package ar.edu.clinica.model;
import jakarta.persistence.*;
@Entity public class Medico extends Registro {
 @Column(nullable=false,length=100) public String nombre;
 @Column(nullable=false,length=100) public String apellido;
 @Column(nullable=false,unique=true,length=30) public String documento;
}
