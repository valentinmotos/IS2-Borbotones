package com.example.mascotas.dto;

import java.util.Date;

public class UsuarioDTO {

    private String id;
    private String mail;
    private String nombre;
    private String apellido;
    private Date alta;
    private Date baja;
    private ZonaDTO zona;
    private FotoDTO foto;

    public UsuarioDTO() {
    }

    public UsuarioDTO(String id, String mail, String nombre, String apellido, Date alta, Date baja,
                      ZonaDTO zona, FotoDTO foto) {
        this.id = id;
        this.mail = mail;
        this.nombre = nombre;
        this.apellido = apellido;
        this.alta = alta;
        this.baja = baja;
        this.zona = zona;
        this.foto = foto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public ZonaDTO getZona() {
        return zona;
    }

    public void setZona(ZonaDTO zona) {
        this.zona = zona;
    }

    public FotoDTO getFoto() {
        return foto;
    }

    public void setFoto(FotoDTO foto) {
        this.foto = foto;
    }
}
