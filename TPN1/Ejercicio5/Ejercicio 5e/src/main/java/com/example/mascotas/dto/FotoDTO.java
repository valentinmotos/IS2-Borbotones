package com.example.mascotas.dto;

public class FotoDTO {

    private String id;
    private String nombre;
    private String mime;
    private byte[] contenido;

    public FotoDTO() {
    }

    public FotoDTO(String id, String nombre, String mime, byte[] contenido) {
        this.id = id;
        this.nombre = nombre;
        this.mime = mime;
        this.contenido = contenido;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }
}
