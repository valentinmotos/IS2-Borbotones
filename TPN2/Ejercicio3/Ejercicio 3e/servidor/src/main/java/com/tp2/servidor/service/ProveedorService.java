package com.tp2.servidor.service;

import com.tp2.servidor.dao.ProveedorDao;
import com.tp2.servidor.dto.ProveedorRequest;
import com.tp2.servidor.entity.Proveedor;
import org.springframework.stereotype.Service;

@Service
public class ProveedorService extends BaseRestService<Proveedor, ProveedorRequest, String> {
    public ProveedorService(ProveedorDao proveedorDao) {
        super(proveedorDao, ProveedorService::convertir, proveedor -> !proveedor.isEliminado());
    }

    private static Proveedor convertir(ProveedorRequest request) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(request.getNombre());
        return proveedor;
    }

    @Override
    protected void validar(Operacion operacion, Proveedor proveedor) {
        if (operacion != Operacion.BAJA && (proveedor.getNombre() == null || proveedor.getNombre().isBlank())) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
    }

    @Override
    protected void actualizar(Proveedor existente, Proveedor nuevo) {
        existente.setNombre(nuevo.getNombre());
    }

    @Override
    protected void marcarEliminada(Proveedor proveedor) {
        proveedor.setEliminado(true);
    }
}
