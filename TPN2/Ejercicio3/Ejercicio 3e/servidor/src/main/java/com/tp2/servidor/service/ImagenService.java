package com.tp2.servidor.service;

import com.tp2.servidor.dao.ImagenDao;
import com.tp2.servidor.dto.ImagenRequest;
import com.tp2.servidor.entity.Imagen;
import org.springframework.stereotype.Service;

@Service
public class ImagenService extends BaseRestService<Imagen, ImagenRequest, String> {
    public ImagenService(ImagenDao imagenDao) {
        super(imagenDao, ImagenService::convertir, imagen -> !imagen.isEliminado());
    }

    private static Imagen convertir(ImagenRequest request) {
        Imagen imagen = new Imagen();
        imagen.setNombre(request.getNombre());
        imagen.setMime(request.getMime());
        imagen.setContenido(request.getContenido());
        return imagen;
    }

    @Override
    protected void validar(Operacion operacion, Imagen imagen) {
        if (operacion != Operacion.BAJA && (imagen.getContenido() == null || imagen.getMime() == null)) {
            throw new IllegalArgumentException("La imagen debe tener contenido y tipo mime");
        }
    }

    @Override
    protected void actualizar(Imagen existente, Imagen nueva) {
        existente.setNombre(nueva.getNombre());
        existente.setMime(nueva.getMime());
        existente.setContenido(nueva.getContenido());
    }

    @Override
    protected void marcarEliminada(Imagen imagen) {
        imagen.setEliminado(true);
    }
}
