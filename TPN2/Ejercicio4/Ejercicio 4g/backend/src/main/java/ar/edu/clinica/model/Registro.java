package ar.edu.clinica.model;
import jakarta.persistence.*;
@MappedSuperclass public abstract class Registro { @Id public String id; public boolean eliminado; }
