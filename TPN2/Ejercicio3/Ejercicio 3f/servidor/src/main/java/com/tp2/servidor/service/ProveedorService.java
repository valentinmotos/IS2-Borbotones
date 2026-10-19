package com.tp2.servidor.service;

import com.tp2.servidor.dao.ProveedorDao;
import com.tp2.servidor.dto.ProveedorRequest;
import com.tp2.servidor.entity.Direccion;
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
        proveedor.setDireccion(request.getDireccion());
        return proveedor;
    }

    @Override
    protected void validar(Operacion operacion, Proveedor proveedor) {
        if (operacion == Operacion.BAJA) {
            return;
        }
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        Direccion direccion = proveedor.getDireccion();
        if (direccion == null || direccion.getLatitud() == null || direccion.getLongitud() == null) {
            throw new IllegalArgumentException("La direccion del proveedor debe tener latitud y longitud");
        }
        if (Math.abs(direccion.getLatitud()) > 90 || Math.abs(direccion.getLongitud()) > 180) {
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90 y la longitud entre -180 y 180");
        }
    }

    @Override
    protected void actualizar(Proveedor existente, Proveedor nuevo) {
        existente.setNombre(nuevo.getNombre());
        existente.setDireccion(nuevo.getDireccion());
    }

    @Override
    protected void marcarEliminada(Proveedor proveedor) {
        proveedor.setEliminado(true);
    }
}
