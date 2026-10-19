package com.tp2.cliente.service;

import com.tp2.cliente.dao.ProveedorDao;
import com.tp2.cliente.dto.ProveedorDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProveedorService {
    private final ProveedorDao proveedorDao;

    public ProveedorService(ProveedorDao proveedorDao) {
        this.proveedorDao = proveedorDao;
    }

    public List<ProveedorDto> listar() {
        return proveedorDao.listar();
    }

    public ProveedorDto buscar(String id) {
        return proveedorDao.buscar(id);
    }

    public void guardar(ProveedorDto proveedor) {
        proveedorDao.guardar(proveedor);
    }

    public void eliminar(String id) {
        proveedorDao.eliminar(id);
    }
}
