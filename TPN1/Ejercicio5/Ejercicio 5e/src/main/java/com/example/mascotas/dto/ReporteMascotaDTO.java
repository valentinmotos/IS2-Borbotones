package com.example.mascotas.dto;

public class ReporteMascotaDTO {

    private String nombreUsuario;
    private String apellidoUsuario;
    private String nombreMascota;
    private long cantidadVotos;

    public ReporteMascotaDTO() {
    }

    public ReporteMascotaDTO(String nombreUsuario, String apellidoUsuario,
                             String nombreMascota, Long cantidadVotos) {
        this.nombreUsuario = nombreUsuario;
        this.apellidoUsuario = apellidoUsuario;
        this.nombreMascota = nombreMascota;
        this.cantidadVotos = cantidadVotos == null ? 0 : cantidadVotos;
    }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getApellidoUsuario() { return apellidoUsuario; }
    public void setApellidoUsuario(String apellidoUsuario) { this.apellidoUsuario = apellidoUsuario; }
    public String getNombreMascota() { return nombreMascota; }
    public void setNombreMascota(String nombreMascota) { this.nombreMascota = nombreMascota; }
    public long getCantidadVotos() { return cantidadVotos; }
    public void setCantidadVotos(long cantidadVotos) { this.cantidadVotos = cantidadVotos; }
}
