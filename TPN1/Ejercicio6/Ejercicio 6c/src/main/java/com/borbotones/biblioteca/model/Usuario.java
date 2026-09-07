package com.borbotones.biblioteca.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "usuarios") @Audited
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank private String dni;
    @NotBlank private String nombre;
    private String telefono;
    @Email private String mail;
    @NotAudited private String clave;
    @Enumerated(EnumType.STRING) private Rol rol = Rol.USER;
    private boolean alta = true;
    @OneToMany(mappedBy = "usuario") @NotAudited private List<Prestamo> prestamos = new ArrayList<>();
    public Long getId() { return id; }
    public String getDni() { return dni; }
    public void setDni(String v) { dni = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { nombre = v; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String v) { telefono = v; }
    public String getMail() { return mail; }
    public void setMail(String v) { mail = v; }
    public String getClave() { return clave; }
    public void setClave(String v) { clave = v; }
    public Rol getRol() { return rol; }
    public void setRol(Rol v) { rol = v; }
    public boolean isAlta() { return alta; }
    public void setAlta(boolean v) { alta = v; }
}
