package com.example.cliente.service;

import com.example.cliente.dao.ReporteDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {
    private final ReporteDAO reporteDAO;

    public ReporteService(ReporteDAO reporteDAO) {
        this.reporteDAO = reporteDAO;
    }

    public ResponseEntity<byte[]> personasPdf() {
        return reporteDAO.personasPdf();
    }

    public ResponseEntity<byte[]> librosExcel() {
        return reporteDAO.librosExcel();
    }
}
