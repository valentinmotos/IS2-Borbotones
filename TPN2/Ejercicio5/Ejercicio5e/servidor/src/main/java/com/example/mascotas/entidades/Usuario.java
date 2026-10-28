package com.example.mascotas.entidades;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import java.util.Date;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"oauth_issuer", "oauth_subject"}))
@Audited
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(length = 254)
    private String mail;
    private String nombre;
    private String apellido;
    @Column(name="oauth_issuer", nullable=false, length=200)
    private String oauthIssuer;
    @Column(name="oauth_subject", nullable=false, length=200)
    private String oauthSubject;
    public String getOauthIssuer() { return oauthIssuer; }
    public void setOauthIssuer(String issuer) { this.oauthIssuer=issuer; }
    public String getOauthSubject() { return oauthSubject; }
    public void setOauthSubject(String subject) { this.oauthSubject=subject; }
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date alta;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date baja;

    @ManyToOne
    @JoinColumn(name = "zona_id")
    private Zona zona;

    @OneToOne
    private Foto foto;

    //Getters y Setters

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }


    public Date getAlta() {
        return alta;
    }

    public void setAlta(Date alta) {
        this.alta = alta;
    }

    public Date getBaja() {
        return baja;
    }

    public void setBaja(Date baja) {
        this.baja = baja;
    }

    public Zona getZona() {
        return zona;
    }

    public void setZona(Zona zona) {
        this.zona = zona;
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public Foto getFoto() {
        return foto;
    }

    public void setFoto(Foto foto) {
        this.foto = foto;
    }
}
