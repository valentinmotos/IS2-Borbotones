package com.example.mascotas.dto;

import com.example.mascotas.enumeracion.Sexo;
import com.example.mascotas.enumeracion.Tipo;

import java.util.Date;

public class MascotaDTO {

    private String id;
    private String nombre;
    private Sexo sexo;
    private Tipo tipo;
    private Date alta;
    private Date baja;
    private UsuarioDTO usuario;
    private FotoDTO foto;

    public MascotaDTO() {
    }

    public MascotaDTO(String id, String nombre, Sexo sexo, Tipo tipo, Date alta, Date baja,
                      UsuarioDTO usuario, FotoDTO foto) {
        this.id = id;
        this.nombre = nombre;
        this.sexo = sexo;
        this.tipo = tipo;
        this.alta = alta;
        this.baja = baja;
        this.usuario = usuario;
        this.foto = foto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Sexo getSexo() { return sexo; }
    public void setSexo(Sexo sexo) { this.sexo = sexo; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public Date getAlta() { return alta; }
    public void setAlta(Date alta) { this.alta = alta; }
    public Date getBaja() { return baja; }
    public void setBaja(Date baja) { this.baja = baja; }
    public UsuarioDTO getUsuario() { return usuario; }
    public void setUsuario(UsuarioDTO usuario) { this.usuario = usuario; }
    public FotoDTO getFoto() { return foto; }
    public void setFoto(FotoDTO foto) { this.foto = foto; }
}
