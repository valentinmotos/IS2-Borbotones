package com.example.mascotas.servicios;

import com.example.mascotas.dto.ReporteMascotaDTO;
import com.example.mascotas.repositorios.MascotaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReporteServicio {

    @Autowired
    private MascotaRepositorio mascotaRepositorio;

    public List<ReporteMascotaDTO> obtenerInformacion() {
        return mascotaRepositorio.generarReporte();
    }
}
