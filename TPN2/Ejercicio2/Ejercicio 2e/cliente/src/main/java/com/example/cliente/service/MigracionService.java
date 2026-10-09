package com.example.cliente.service;

import com.example.cliente.dao.MigracionDAO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class MigracionService {
    private final MigracionDAO migracionDAO;

    public MigracionService(MigracionDAO migracionDAO) {
        this.migracionDAO = migracionDAO;
    }

    public int importar(MultipartFile archivo) throws IOException {
        return migracionDAO.importar(archivo);
    }
}
