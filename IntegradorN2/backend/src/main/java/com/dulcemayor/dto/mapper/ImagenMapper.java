package com.dulcemayor.dto.mapper;

import com.dulcemayor.dto.ImagenDTO;
import com.dulcemayor.dto.ImagenCargaDTO;
import com.dulcemayor.entity.Imagen;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ImagenMapper implements CrudMapper<Imagen, ImagenCargaDTO, ImagenDTO> {

    @Override
    public ImagenDTO aRespuesta(Imagen imagen) {
        return new ImagenDTO(imagen.getId(), imagen.getNombre(), imagen.getMime(), imagen.getTipoImagen(),
                "/api/imagenes/" + imagen.getId());
    }

    @Override
    public List<ImagenDTO> aRespuestas(List<Imagen> imagenes) {
        return imagenes.stream().map(this::aRespuesta).toList();
    }

    @Override
    public void actualizar(ImagenCargaDTO request, Imagen imagen) {
        // Las imágenes no se modifican mediante el CRUD genérico.
    }
}
